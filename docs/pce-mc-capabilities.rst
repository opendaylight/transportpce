.. _pce-mc-capabilities:

Spectrum Assignment Using MC Capabilities
=========================================

This document explains how TransportPCE uses OpenROADM media channel (MC)
capabilities to select a service frequency slot when computing an optical service path.
The central method is ``getSpectrumAssignment`` in ``PostAlgoPathValidator``.


Background
----------

OpenROADM 2.2.1 introduced four MC capability attributes that describe what a
port can support in terms of optical spectrum. These are listed directly on each
ROADM degree and SRG node.

OpenROADM 7.1 introduced a named ``mc-capability-profile`` list at the
device root. Nodes reference one or more profiles by name via a
``leaf-list mc-capability-profile-name``, making profiles reusable across ports
with identical characteristics. XPDR network ports gained MC capability support
in this version. 7.1 also introduced ``min-edge-freq`` and ``max-edge-freq`` to
describe a port's physical frequency range.

MC capability handling is implemented for OpenROADM version **2.2.1** and **7.1** only.
Capabilities are read during port mapping and stored in the tpce portmapping data store.

.. list-table::
   :header-rows: 1
   :widths: 30 25 15

   * - Attribute
     - Unit / default
     - Versions
   * - ``center-freq-granularity``
     - GHz, default 50
     - 2.2.1, 7.1
   * - ``slot-width-granularity``
     - GHz, default 50
     - 2.2.1, 7.1
   * - ``min-slots``
     - default 1
     - 2.2.1, 7.1
   * - ``max-slots``
     - default 1
     - 2.2.1, 7.1
   * - ``min-edge-freq``
     - THz, optional
     - 7.1
   * - ``max-edge-freq``
     - THz, optional
     - 7.1

During path computation ``getSpectrumAssignment`` reads the stored capabilities
from each node on the candidate path and uses them to find a valid frequency
assignment.


The Spectrum Grid
-----------------

TransportPCE models the optical spectrum as a ``BitSet`` of 768 slots. Each
slot is 6.25 GHz wide and the grid starts at 191.325 THz:

.. code-block:: none

   slot 0   →  191.325 000 THz
   slot 1   →  191.331 250 THz
   slot 2   →  191.337 500 THz
   ...
   slot 767 →  196.118 750 THz
   (upper edge: 196.125 THz)

A bit set to ``1`` means the slot is available; ``0`` means occupied or
excluded. The full grid therefore starts as a 768-bit set with all bits set.


Visualizing MC Capability Profiles
------------------------------------

The diagrams below illustrate how ``mc-capability-profile`` attributes
map onto the optical spectrum. These correspond to the constraints
applied in Phases 3–5 of ``getSpectrumAssignment``.


ROADM nodes (InterfaceMcCapability)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. code-block:: none

      ←────────────── Full C-band (191.325–196.125 THz) ──────────────→

           min-edge-freq                             max-edge-freq
                 ↓                                         ↓
   ──────────────┬─────────────────────────────────────────┬─────────────
                 │                Usable Band              │
                 ├──┬──┬──┬──┬──┬──┬──┬──┬──┬──┬──┬──┬──┬──┤
                 │  │  │  │  │  │  │  │  │  │  │  │  │  │  │
                 └──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┘

                 │←→│  Slot-width granularity
                 │←────────────→│  Min slots × granularity
                 │←──────────────────────────────→│  Max slots × granularity

                      ↑      ↑      ↑      ↑      ↑
                      └──────┴──────┴──────┴──────┘
                       Center-frequency granularity


``min-edge-freq`` / ``max-edge-freq``
   Defines the physical passband of the port. Slots outside this
   band are excluded from spectrum assignment (Phase 3).

``slot-width-granularity``
   The unit step for service width. A service width must be an
   exact multiple of this value (Phase 4).

``min-slots`` / ``max-slots``
   The allowed range of slot-width-granularity steps a service may
   occupy. A width outside [min × swg, max × swg] is rejected
   (Phase 4).

``center-freq-granularity``
   The allowed spacing between center frequencies. The path-level alignment
   constraint is the least common multiple (LCM) of all nodes’
   center-frequency granularities, ensuring that valid center frequencies
   lie on every node’s frequency grid (Phase 5).


XPDR nodes (XpdrMcCapability)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

For 7.1 XPDRs only three attributes apply:

 * ``center-freq-granularity``
 * ``min-edge-freq``
 * ``max-edge-freq``

.. code-block:: none

      ←────────────── Full C-band (191.325–196.125 THz) ──────────────→

           min-edge-freq                               max-edge-freq
                 ↓                                           ↓
   ──────────────┬───────────────────────────────────────────┬─────────────
                 │                                           │
                 │            Tunable Laser Range            │
                 │                                           │
                 └───────────────────────────────────────────┘
                       ↑      ↑      ↑      ↑      ↑
                       └──────┴──────┴──────┴──────┘
                          Center-Frequency Granularity
                        (minimum center-frequency spacing)


``min-edge-freq`` / ``max-edge-freq``
   The physical tuning range of the laser. Applied in Phase 3
   identically to ROADM nodes.

``center-freq-granularity``
   The minimum spacing between selectable center frequencies.
   Contributes to the path-level LCM in Phase 5.


How getSpectrumAssignment Works
--------------------------------

The method walks through five phases for each candidate path.


Phase 1: Collect path nodes
~~~~~~~~~~~~~~~~~~~~~~~~~~~

The method iterates the path's edge list and collects the unique set of PCE
nodes (by source and destination node ID of each edge). This deduplication
means a node that appears in multiple edges is only processed once.


Phase 2: Build the available frequency bitmap
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Starting from a full 768-bit set (all slots available), the method ANDs the
frequency bitmap of each non-contentionless node into the running result:

.. code-block:: none

   result = all-ones (768 bits)
   for each node:
       result = result AND node.frequencyBitmap

The result is the intersection of available slots across all nodes on the
path. If the result is empty at this point, no frequencies are available and
the method returns an empty assignment immediately.

A node marked as a *contentionless SRG* is skipped in this phase because
contention-less switching fabrics do not share spectrum resources between
add/drop ports — occupancy on one port does not exclude a slot on another.


Phase 3: Apply frequency range constraints
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Each node carries a ``FrequencyRange`` that describes the portion of the
spectrum grid it can physically support. This is derived from ``min-edge-freq``
and ``max-edge-freq`` in the mc-capability-profile:

- If both are present, ``McCapabilityRange`` maps the interval
  ``[min-edge-freq, max-edge-freq]`` to a BitSet of the slots that fall
  within that range.
- If either is absent, ``McCapabilityRange.from()`` falls back to
  ``EntireGridRange``, which sets all 768 bits and imposes no restriction.

The method ANDs all nodes' frequency ranges into the running result:

.. code-block:: none

   for each node:
       result = result AND node.supportableFrequencyRange(grid parameters)

If the result is empty after this step, the path is rejected with the message
*"No frequencies available (restricted by McCapabilities)"*.


Phase 4: Validate service slot width
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The requested service width (in slots) must be compatible with every node's
``slot-width-granularity``, ``min-slots``, and ``max-slots``. The check is:

.. code-block:: none

   serviceWidthGHz  = slotCount × 6.25 GHz
   slotsPerStep     = serviceWidthGHz / node.slotWidthGranularity
   compatible       = serviceWidthGHz is an exact multiple of slotWidthGranularity
                      AND minSlots ≤ slotsPerStep ≤ maxSlots

This check is applied to every node in the collection. If any node fails, the
method returns an empty assignment.

XPDR nodes are represented by ``XpdrMcCapability``, which always returns
``true`` for this check. The slot-width and slot-count limits in an XPDR
mc-capability-profile reflect client-side constraints, not optical line-side
constraints, so they are intentionally excluded from spectrum slot-width
validation.


Phase 5: Select center frequency
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The effective center frequency granularity for the path is the least common
multiple (LCM) of all nodes' ``center-freq-granularity`` values, expressed in
slots. The assignment algorithm (high-to-low) selects the highest-indexed
contiguous block of ``slotCount`` available bits whose center frequency falls
on a multiple of this combined granularity relative to the ITU-T G.694.1
reference frequency of 193.1 THz.

The result is a ``SpectrumAssignment`` with ``beginIndex`` and ``stopIndex``
(both inclusive). An empty assignment (``beginIndex = stopIndex = 0``) signals
failure.

.. code-block:: none

    Center-frequency granularity (CFG) anchored at 193.1 THz (ITU-T G.694.1)

      ▼   denotes the 193.1 THz reference frequency
      |.| denotes one 6.25 GHz spectrum slot

             191.325   ...   193.05          193.1           193.15    ...   196.125
    CFG 50GHz   |               ↓                               ↓               |
    CFG 25GHz   |               ↓       ↓       ▼       ↓       ↓               |
                |.|.|  ...  |.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|  ...  |.|.|
                |←------------------- 768 × 6.25 GHz slots --------------------→|


Examples
--------

The examples below correspond directly to the test scenarios in
``PostAlgoPathValidatorTest``. All paths assume the full 768-slot bitmap is
available on every node unless stated otherwise.


Example 1: Uniform ROADM path — 100 GHz service
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Path:** ROADM-A-SRG4 → ROADM-A-DEG1 → ROADM-B-DEG1 → ROADM-B-SRG3

.. list-table::
   :header-rows: 1
   :widths: 30 20 20 15 15

   * - Node
     - center-freq-gran.
     - slot-width-gran.
     - min-slots
     - max-slots
   * - ROADM-A-SRG4
     - 6.25 GHz
     - 12.5 GHz
     - 3
     - 16
   * - ROADM-A-DEG1
     - 6.25 GHz
     - 12.5 GHz
     - 3
     - 16
   * - ROADM-B-DEG1
     - **100.0 GHz**
     - 12.5 GHz
     - 3
     - 16
   * - ROADM-B-SRG3
     - 6.25 GHz
     - 12.5 GHz
     - 3
     - 16

**Service:** 100 GHz → 16 slots × 6.25 GHz

*Phase 2 (bitmap):* All nodes report full spectrum → result is all 768 bits.

*Phase 3 (frequency range):* No min/max-edge-freq on any node → no
restriction.

*Phase 4 (slot-width check):* 100 GHz / 12.5 GHz = 8 steps per slot-width
unit; 3 ≤ 8 ≤ 16 on all nodes → compatible.

*Phase 5 (center frequency):* LCM(6.25, 6.25, 100.0, 6.25) = 100 GHz = 16
slots per step. Starting from the top of the grid the algorithm finds center
slot 748 (196.0 THz = 193.1 + 29 × 0.1 THz), giving a 16-slot window at
slots 740–755.

**Result:** ``beginIndex=740, stopIndex=755``


Example 2: Mixed center-freq-granularity — 62.5 GHz service
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Path:** ROADM-B-SRG13 → ROADM-B-DEG2 → ROADM-C-DEG2 → ROADM-C-SRG13

.. list-table::
   :header-rows: 1
   :widths: 18 20 20 12 12 18

   * - Node
     - center-freq-gran.
     - slot-width-gran.
     - min-slots
     - max-slots
     - 62.5 GHz service
   * - ROADM-B-SRG13
     - 6.25 GHz
     - 12.5 GHz
     - 1
     - 20
     - Pass
   * - ROADM-B-DEG2
     - **75.0 GHz**
     - 12.5 GHz
     - 4
     - 8
     - Pass
   * - ROADM-C-DEG2
     - 6.25 GHz
     - 12.5 GHz
     - 1
     - 20
     - Pass
   * - ROADM-C-SRG13
     - 6.25 GHz
     - 12.5 GHz
     - 1
     - 20
     - Pass

**Service:** 62.5 GHz → 10 slots × 6.25 GHz

*Phase 4 (slot-width check):* 62.5 GHz / 12.5 GHz = 5; 4 ≤ 5 ≤ 8 on all
nodes → compatible.

*Phase 5 (center frequency):* LCM(6.25, 75.0, 6.25, 6.25) = 75 GHz = 12
slots per step. The algorithm selects the highest 10-slot block whose center
aligns to a multiple of 75 GHz from 193.1 THz.

**Result:** ``beginIndex=747, stopIndex=756``

Compare this to Example 1: even though 62.5 GHz < 100 GHz, the 75 GHz center
frequency constraint from ROADM-B-DEG2 limits where the block can be placed.


Example 3: min-slots and max-slots reject a service
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Path:** ROADM-B-SRG13 → ROADM-B-DEG2 → ROADM-C-DEG2 → ROADM-C-SRG13 → ROADM-C-SRG12

.. list-table::
   :header-rows: 1
   :stub-columns: 1
   :widths: 18 20 20 12 12 18

   * - Node
     - center-freq-gran.
     - slot-width-gran.
     - min-slots
     - max-slots
     - 62.5 GHz service
   * - ROADM-B-SRG13
     - 6.25 GHz
     - **12.5 GHz**
     - 1
     - **4**
     - **Fail**
   * - ROADM-B-DEG2
     - 75.0 GHz
     - 12.5 GHz
     - 4
     - 8
     - Pass
   * - ROADM-C-DEG2
     - 6.25 GHz
     - 12.5 GHz
     - 1
     - 20
     - Pass
   * - ROADM-C-SRG12
     - 50.0 GHz
     - **50.0 GHz**
     - 1
     - 1
     - **Fail**

* A node that advertises ``slot-width-granularity=12.5 GHz, min-slots=1, max-slots=4``
  supports service widths from 12.5 GHz to 50 GHz in 12.5 GHz increments.
  ROADM-B-SRG13 therefore fails the phase 4 slot-width check.
* A node that advertises ``slot-width-granularity=50 GHz, max-slots=1`` is
  saying it can carry exactly one 50 GHz channel and nothing wider or narrower.
  ROADM-C-SRG12 therefore fails the phase 4 slot-width check.


The service request must fit within all nodes' slot ranges simultaneously.


Example 4: min-edge-freq and max-edge-freq restricting usable band
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Path:** XPDR-A2 → ROADM-A-DEG1 → ROADM-B-DEG1 → ROADM-B-SRG3

.. list-table::
   :header-rows: 1
   :widths: 30 25 25 20

   * - Node
     - min-edge-freq
     - max-edge-freq
     - center-freq-gran.
   * - XPDR-A2
     - **191.325 THz**
     - **194.45 THz**
     - 6.25 GHz
   * - ROADM-A-DEG1
     - —
     - —
     - 6.25 GHz
   * - ROADM-B-DEG1
     - —
     - —
     - **100.0 GHz**
   * - ROADM-B-SRG3
     - —
     - —
     - 6.25 GHz

XPDR-A2 advertises ``min-edge-freq=191.325 THz`` and
``max-edge-freq=194.45 THz``, which maps to slots 0–499 (the lower 65 % of
the grid). All ROADM nodes have full spectrum available.

**Service:** 100 GHz → 16 slots

*Phase 3 (frequency range):* XPDR-A2's range covers slots 0–499. After
ANDing all nodes' ranges, slots 500–767 are excluded.

*Phase 4 (slot-width check):* XPDR uses ``XpdrMcCapability`` which always
passes → compatible.

*Phase 5 (center frequency):* With ROADM-B-DEG1 imposing a 100 GHz
center-frequency step (16 slots), the highest valid 16-slot window within
slots 0–499 has its center at slot 492 (194.4 THz = 193.1 + 13 × 0.1 THz).

**Result:** ``beginIndex=484, stopIndex=499``

Without the XPDR frequency restriction the same path would yield
``beginIndex=740, stopIndex=755`` (see Example 1). The restriction shifts the
assignment to the highest valid position within the supported band.


MC Capability Implementations
-------------------------------

TransportPCE uses three ``McCapability`` implementations:

.. list-table::
   :header-rows: 1
   :widths: 30 70

   * - Class
     - Used for
   * - ``InterfaceMcCapability``
     - ROADM degrees and SRGs. Enforces ``slot-width-granularity``,
       ``min-slots``, and ``max-slots`` in the slot-width check, and
       applies ``min/max-edge-freq`` as a ``FrequencyRange``.
   * - ``XpdrMcCapability``
     - XPDR network ports. Always passes the slot-width check.
       Applies ``min/max-edge-freq`` as a ``FrequencyRange``.
   * - ``UnconstrainedMcCapability``
     - OTN nodes, which impose no optical spectrum constraints.
       Always passes all checks and returns ``EntireGridRange``.

The ``FrequencyRange`` abstraction has two implementations:

.. list-table::
   :header-rows: 1
   :widths: 30 70

   * - Class
     - Behaviour
   * - ``McCapabilityRange``
     - Maps a ``[min-edge-freq, max-edge-freq]`` interval to a BitSet of
       the grid slots that fall within the interval. Slots outside the
       interval are cleared.
   * - ``EntireGridRange``
     - Sets all 768 bits. Used when no frequency range is advertised.


Related Classes
---------------

* ``PostAlgoPathValidator.getSpectrumAssignment`` — main entry point
* ``McCapabilityCollection.usableFrequencyRange`` — Phase 3 implementation
* ``McCapabilityCollection.isCompatibleService`` — Phase 4 implementation
* ``McCapabilityRange`` / ``EntireGridRange`` — frequency range models
* ``CenterFrequencyGranularityCollection`` — computes the effective LCM
  center frequency granularity across all path nodes
* ``PortMappingVersion710`` — reads mc-capability-profile from the device
  and stores ``min/max-edge-freq`` in the portmapping data store (7.1 only)

Known limitations
-----------------

The slot-width check requires the service width to be an exact multiple of each
node's ``slot-width-granularity``. A service request is therefore rejected if
any node on the path advertises a granularity that does not divide the
requested width evenly — for example, a 75 GHz service on a path containing a
node with ``slot-width-granularity=100 GHz``. This check is performed by
``McCapabilityCollection.isCompatibleService``.
