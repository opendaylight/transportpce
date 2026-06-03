.. _pce-mc-capabilities-code-reference:

MC Capabilities — Code Reference
=================================

This document walks through the Java classes that implement spectrum assignment
via MC capabilities. For the algorithm overview, phases, and examples see
:ref:`pce-mc-capabilities`.

All classes live under
``pce/src/main/java/org/opendaylight/transportpce/pce/``.


Package layout
--------------

.. code-block:: text

   pce/spectrum/
   ├── slot/
   │   ├── McCapability.java                    ← per-node capability interface
   │   ├── InterfaceMcCapability.java           ← ROADM degrees and SRGs
   │   ├── XpdrMcCapability.java                ← XPDR network ports (7.1)
   │   ├── UnconstrainedMcCapability.java       ← OTN nodes
   │   ├── CapabilityCollection.java            ← collection interface
   │   └── McCapabilityCollection.java          ← collection implementation
   ├── range/
   │   ├── FrequencyRange.java                  ← frequency range interface
   │   ├── McCapabilityRange.java               ← bounded by min/max-edge-freq
   │   └── EntireGridRange.java                 ← no frequency restriction
   ├── centerfrequency/
   │   ├── Collection.java                      ← center-freq-granularity interface
   │   └── CenterFrequencyGranularityCollection.java ← LCM computation
   ├── index/
   │   ├── Base.java / BaseFrequency.java       ← reference frequency → slot index
   │   ├── Index.java / SpectrumIndex.java      ← first/last valid center slot
   │   └── NoIndexFoundException.java
   ├── assignment/
   │   ├── Assign.java / AssignSpectrumHighToLow.java ← slot selection algorithm
   │   ├── Range.java / IndexRange.java         ← selected slot range
   └── observer/
       ├── Observer.java                        ← error reporting interface
       └── VoidObserver.java                    ← no-op implementation

   frequency/
   ├── Select.java                              ← frequency selection interface
   └── FrequencySelectionFactory.java           ← client-input + node bitmap merge

   graph/
   └── PostAlgoPathValidator.java               ← getSpectrumAssignment entry point

.. _mc_capability_interface:

McCapability — per-node capability interface
--------------------------------------------

``McCapability`` (``spectrum/slot/McCapability.java``) defines the contract that
each node on the path must satisfy:

.. code-block:: text

              ┌──────────────────────────────────────────────────────┐
              │                      «interface»                     │
              │                      McCapability                    │
              ├──────────────────────────────────────────────────────┤
              │  + centerFrequencyGranularity()       : BigDecimal   │
              │  + isCompatibleWithServiceFrequency() : boolean      │
              │  + supportableFrequencyRange()        : BitSet       │
              └───────────────────────────┬──────────────────────────┘
                                          │
                ┌─────────────────────────┼─────────────────────────┐
                │                         │                         │
                ▼                         ▼                         ▼
    ┌───────────────────────┐  ┌──────────────────────┐  ┌───────────────────────────┐
    │ InterfaceMcCapability │  │ XpdrMcCapability     │  │ UnconstrainedMcCapability │
    │ (ROADM degree / SRG)  │  │ (XPDR 7.1)           │  │ (OTN)                     │
    ├───────────────────────┤  ├──────────────────────┤  ├───────────────────────────┤
    │ slotWidthGran.        │  │ centerFreqGran.      │  │ (no constraints;          │
    │ centerFreqGran.       │  │ freqRange            │  │  all checks pass)         │
    │ minSlots / maxSlots   │  ├──────────────────────┤  └───────────────────────────┘
    │ freqRange             │  │ isCompatible()       │
    ├───────────────────────┤  │   → always true      │
    │ isCompatible()        │  └──────────────────────┘
    │   (validates width)   │
    └───────────────────────┘

.. code-block:: java

   BigDecimal centerFrequencyGranularity();

   boolean isCompatibleWithServiceFrequency(double requiredFrequencyWidthGHz);
   boolean isCompatibleWithServiceFrequency(double requiredFrequencyWidthGHz, Observer observer);

   BitSet supportableFrequencyRange(double slotWidthGranularityGHz,
                                    double edgeFrequencyTHz,
                                    int effectiveBits);

``centerFrequencyGranularity()`` feeds Phase 5 (LCM computation).
``isCompatibleWithServiceFrequency()`` is the Phase 4 slot-width check.
``supportableFrequencyRange()`` returns the Phase 3 frequency range bitmap.

The ``Observer`` parameter lets the collection report a human-readable error
message when a check fails. ``VoidObserver`` discards these messages silently;
the real subscriber passed into ``getSpectrumAssignment`` is wired in through
``McCapabilityCollection``.

.. _interface-mc-capability-roadm-nodes:

InterfaceMcCapability — ROADM nodes
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Used for ROADM degree and SRG nodes. Fields:

* ``slotWidthGranularity`` — from ``slot-width-granularity`` in the mc-capability-profile.
* ``centerFrequencyGranularity`` — from ``center-freq-granularity``.
* ``minSlots`` / ``maxSlots`` — slot count bounds.
* ``supportedFrequencyRange`` — a ``FrequencyRange`` (see below).

The slot-width check (``isCompatibleWithServiceFrequency``):

.. code-block:: java

   // Phase 4 — InterfaceMcCapability.java:96-117
   BigDecimal remainder = requiredFrequencyWidthGHz.remainder(slotWidthGranularity);
   if (remainder.compareTo(BigDecimal.ZERO) != 0) {
       observer.error(...);
       return false;                  // width is not a multiple of granularity
   }
   BigDecimal quotient = requiredFrequencyWidthGHz.divideToIntegralValue(slotWidthGranularity);
   if (quotient >= minSlots && quotient <= maxSlots) {
       return true;                   // nr of steps is within [minSlots, maxSlots]
   }
   observer.error(...);
   return false;

The frequency range bitmap (``supportableFrequencyRange``) simply delegates:

.. code-block:: java

   // InterfaceMcCapability.java:135-141
   return supportedFrequencyRange.gridRange(slotWidthGranularityGHz, edgeFrequencyTHz, effectiveBits);

.. _xpdr-mc-capability-xpdr-nodes:

XpdrMcCapability — XPDR nodes
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Used for OpenROADM 7.1 XPDR network ports. Only two fields are relevant:

* ``centerFreqGranularity``
* ``supportedFrequencyRange`` (same ``FrequencyRange`` abstraction)

``isCompatibleWithServiceFrequency`` always returns ``true``:

.. code-block:: java

   // XpdrMcCapability.java:72-76
   @Override
   public boolean isCompatibleWithServiceFrequency(BigDecimal requiredFrequencyWidthGHz, Observer observer) {
       return true;
   }

The OpenROADM 7.1 White Paper (Section 4.8.3) states that
``slot-width-granularity``, ``min-slots``, and ``max-slots`` from an XPDR
mc-capability-profile describe client-side electrical constraints, not optical
line-side spectrum constraints. The XPDR implementation therefore skips the
slot-width check entirely.


UnconstrainedMcCapability — OTN nodes
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

A null-object implementation. Both ``isCompatibleWithServiceFrequency`` and
``supportableFrequencyRange`` return unconstrained results (``true`` /
all-ones BitSet). ``centerFrequencyGranularity()`` returns ``null``, which
``CenterFrequencyGranularityCollection.add`` silently ignores.


FrequencyRange — frequency range interface
------------------------------------------

``FrequencyRange`` (``spectrum/range/FrequencyRange.java``) has one method:

.. code-block:: java

   BitSet gridRange(double slotWidthGranularityGHz, double edgeFrequencyTHz, int effectiveBits);

It maps a frequency range onto the slot grid, returning a BitSet where each
set bit marks an available slot.


McCapabilityRange — bounded range
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Constructed from an explicit ``[minFrequency, maxFrequency]`` interval. The
mapping to slot indices:

.. code-block:: text

   // McCapabilityRange.java:gridRange()
   BigDecimal slotWidthTHz = slotWidthGranularityGHz * 0.001;
   BigDecimal gridMin      = edgeFrequencyTHz;
   BigDecimal gridMax      = gridMin + effectiveBits * slotWidthTHz;

   BigDecimal minDiff = max(minFrequency, gridMin) - gridMin;
   BigDecimal maxDiff = min(maxFrequency, gridMax) - gridMin;

   int minIndex = ceil(minDiff / slotWidthTHz);   // RoundingMode.UP
   int maxIndex = floor(maxDiff / slotWidthTHz);  // RoundingMode.DOWN

   bitSet.set(minIndex, maxIndex);                // [minIndex, maxIndex)

The range is clamped to the actual grid boundaries by the ``max`` / ``min``
calls. Rounding up the lower edge and down the upper edge ensures that a slot
is only included when it falls fully within the supported band.

The static factory ``McCapabilityRange.from()`` handles absent attributes:

.. code-block:: text

   minFrequencyTHz == null && maxFrequencyTHz == null → EntireGridRange
   minFrequencyTHz == null                            → use edgeFrequencyTHz as lower bound
   maxFrequencyTHz == null                            → use (edgeFrequencyTHz + grid width) as upper bound


EntireGridRange — no restriction
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Sets all ``effectiveBits`` bits. Used when a node advertises no
``min/max-edge-freq`` or belongs to a node type (OTN) that carries no
frequency constraint.


McCapabilityCollection — aggregate Phase 3 and 4
-------------------------------------------------

``McCapabilityCollection`` holds one ``McCapability`` per path node and
implements ``CapabilityCollection``:

**Phase 4 — slot-width compatibility check:**

.. code-block:: java

   // McCapabilityCollection.java:isCompatibleService()
   BigDecimal widthGHz = slotWidthGranularityGHz * slotCount;
   for (McCapability cap : slots) {
       if (!cap.isCompatibleWithServiceFrequency(widthGHz, observer)) {
           return false;
       }
   }
   return true;

A single failing node short-circuits the loop.

**Phase 3 — usable frequency range:**

.. code-block:: java

   // McCapabilityCollection.java:usableFrequencyRange()
   BitSet result = availableFrequencyGrid.clone();
   for (McCapability cap : slots) {
       result.and(cap.supportableFrequencyRange(slotWidthGranularityGHz,
                                                edgeFrequencyTHz, effectiveBits));
   }
   return result;

The running AND accumulates each node's range until only slots supported by
every node on the path remain.


.. _cfg-collection-examples:

CenterFrequencyGranularityCollection — Phase 5
-----------------------------------------------

Stores all ``center-freq-granularity`` values seen on the path (in Hz
internally for integer arithmetic) and computes their LCM.

The key output for Phase 5 is ``slots()``. The two examples below correspond
to the diagrams in :ref:`pce-mc-capabilities`.

**CFG = 25 GHz** — effective step = LCM(6.25, 25) = 25 GHz = 4 slots:

.. code-block:: java

    Collection centerFrequencyGranularityCollection = new CenterFrequencyGranularityCollection(50);
    centerFrequencyGranularityCollection.add(6.25);
    centerFrequencyGranularityCollection.add(6.25); // duplicate values are ignored
    centerFrequencyGranularityCollection.add(25);

    // Distance between center frequencies, i.e. the nr of 6.25 GHz slots: 4
    int centerFrequencyGranularity = centerFrequencyGranularityCollection.slots(
            GridConstant.GRANULARITY);

**CFG = 75 GHz** — effective step = LCM(25, 75) = 75 GHz = 12 slots:

.. code-block:: java

    Collection centerFrequencyGranularityCollection = new CenterFrequencyGranularityCollection(50);
    centerFrequencyGranularityCollection.add(25);
    centerFrequencyGranularityCollection.add(75);

    // Distance between center frequencies, i.e. the nr of 6.25 GHz slots: 12 (6.25 × 12 = 75 GHz)
    int centerFrequencyGranularity = centerFrequencyGranularityCollection.slots(
            GridConstant.GRANULARITY);

193.1 THz reference frequency
-----------------------------

The reference frequency is primarily needed to find the highest
possible center frequency (S). Finding the location of the reference
frequency on the spectrum grid:

.. code-block:: java

    // ▼ - The location of the 193.1 THz reference frequency
    Base baseFrequency = new BaseFrequency();
    int baseFrequencyIndex = baseFrequency.referenceFrequencySpectrumIndex(
            GridConstant.CENTRAL_FREQUENCY_THZ,
            GridConstant.START_EDGE_FREQUENCY_THZ,
            GridConstant.GRANULARITY
    );

The iteration starting point (S)
--------------------------------

In the example below, the service requires 87.5 GHz of bandwidth, which means
the iteration must start below the highest valid center frequency granularity
to leave room for the half-width on each side. The location of S depends primarily
on the center frequency granularity combined with required service bandwidth.

.. code-block:: text

      L   : spectrum grid lower edge frequency
      U   : spectrum grid upper edge frequency
      ▼   : the 193.1 THz reference frequency
      |.| : an occupied 6.25 GHz spectrum slot
      `-´ : possible 87.5 GHz wide service slot
      S   : iteration start point
      ↓   : 25 GHz center frequency granularity

      CFG    L                                                               S               U
      25 GHz |       ↓       ↓       ↓       ↓       ▼       ↓       ↓       ↓       ↓       |
             |.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|.|
                                                               `---------------------------´

Finding the iteration starting point is dependent on...

* The 193.1 THz reference frequency (i.e. denoted as ▼ above)
* Center frequency granularity (i.e. the distance between ▼ and ↓ above)
* Service frequency width (i.e. nr of slots encapsulated by ```---------------------------´``)
* The total nr of bits on the frequency grid (e.g. 768)

Finding S can be expressed in code like this:

.. code-block:: java

    // ▼ - The location of the 193.1 THz reference frequency
    Base baseFrequency = new BaseFrequency();
    int baseFrequencyIndex = baseFrequency.referenceFrequencySpectrumIndex(
            GridConstant.CENTRAL_FREQUENCY_THZ,
            GridConstant.START_EDGE_FREQUENCY_THZ,
            GridConstant.GRANULARITY
    );

    // S - The iteration start point, i.e. the last usable index (highest value)
    //     of the center frequency granularity on the frequency grid
    SpectrumIndex spectrumIndex = new SpectrumIndex();
    int lastCenterFrequencyIndex = spectrumIndex.lastCenterFrequencyIndex(
            centerFrequencyGranularity, //e.g. 25 GHz
            baseFrequencyIndex,         //See '193.1 THz reference frequency'
            serviceSlotWidth,           //e.g. 14 (for a 87.5 GHz wide service slot)
            effectiveBits               //e.g. 768 (the nr of effective bits on the grid)
    );


AssignSpectrumHighToLow — slot selection
-----------------------------------------

Implements the high-to-low center-frequency scan:

.. code-block:: java

   // AssignSpectrumHighToLow.java:range()
   int lastCenter = slotIndex.lastCenterFrequencyIndex(
           centerFrequencyGranularity, baseFrequencyIndex, serviceSlotWidth, effectiveBits);

   BitSet reference = new BitSet(serviceSlotWidth);
   reference.set(0, serviceSlotWidth);         // all-ones reference window

   int half = serviceSlotWidth / 2;
   for (int center = lastCenter; center >= half; center -= centerFrequencyGranularity) {
       int lo = center - half;
       int hi = center + half;
       if (spectrumOccupation.get(lo, hi).equals(reference)) {
           return new IndexRange(lo, hi - 1);  // found: [lo, hi) → [lo, hi-1] inclusive
       }
   }
   return new IndexRange(0, 0);                // not found

``SpectrumIndex.lastCenterFrequencyIndex()`` finds the highest slot index that:

* is aligned to a multiple of ``centerFrequencyGranularity`` relative to
  ``baseFrequencyIndex`` (193.1 THz, slot 284), and
* leaves room for the half-width on each side (i.e.
  ``effectiveBits - center ≥ half``).

``SpectrumIndex.firstCenterFrequencyIndex()`` mirrors this for the low end.


FrequencySelectionFactory — client-input overlay
-------------------------------------------------

Before spectrum assignment the method applies client-driven restrictions via
``FrequencySelectionFactory.availableFrequencies()``:

.. code-block:: java

   // FrequencySelectionFactory.java
   BitSet available = availableFrequenciesOnNodes.clone();
   if (availableCustomerRange != null) {
       available.and(availableCustomerRange);    // customer restriction
   }
   BitSet rangeResult = intersectionLimitation.intersection(available);  // client range wish
   return subsetLimitation.subset(rangeResult);                          // client specific assignment

``clientInput.clientRangeWishListIntersection()`` represents an optional
frequency range the API caller wants to restrict assignment to (intersection).
``clientInput.clientRangeWishListSubset()`` represents an optional specific
assignment the caller wants (e.g., a requested center frequency / slot count
pair — a hard requirement that must be a subset of the available range).
``spectrumConstraint`` is an optional per-customer allowed range.

In the absence of such constraints both collections act as no-ops and the
result equals the MC-filtered bitmap.


getSpectrumAssignment — annotated call sequence
-----------------------------------------------

Putting it all together, here is the method body with each step mapped to
its phase:

.. code-block:: java

   // PostAlgoPathValidator.java:1082-1181 (simplified)
   public SpectrumAssignment getSpectrumAssignment(...) {

       // Phase 1 — collect unique PceNodes from every edge in the path
       Set<PceNode> pceNodes = new LinkedHashSet<>();
       for (PceGraphEdge edge : path.getEdgeList()) {
           pceNodes.add(allPceNodes.get(edge.link().getSourceId()));
           pceNodes.add(allPceNodes.get(edge.link().getDestId()));
       }

       Collection cfgCollection = new CenterFrequencyGranularityCollection(50);
       CapabilityCollection mcCollection = new McCapabilityCollection(errorObserver);

       for (PceNode node : pceNodes) {

           // Phase 2 — AND each non-contentionless node's frequency bitmap
           if (!node.isContentionLessSrg()) {
               result.and(node.getBitSetData());
           }

           McCapability cap = node.mcCapabilities();
           cfgCollection.add(cap.centerFrequencyGranularity());  // feeds Phase 5
           mcCollection.add(cap);                                 // feeds Phase 3 + 4
       }

       if (result.isEmpty()) return createEmptySpectrumAssignment();  // Phase 2 early exit

       // Phase 3 — AND each node's supported frequency range (min/max-edge-freq)
       result = mcCollection.usableFrequencyRange(result, GRANULARITY,
               START_EDGE_FREQUENCY_THZ, EFFECTIVE_BITS);
       if (result.isEmpty()) return createEmptySpectrumAssignment();

       // Phase 4 — reject if any node's slot-width constraints reject the service
       int slotCount = clientInput.slotWidth(spectralWidthSlotNumber);
       if (!mcCollection.isCompatibleService(GRANULARITY, slotCount)) {
           return createEmptySpectrumAssignment();
       }

       // Apply client-input and customer-range overlays
       BitSet assignable = new FrequencySelectionFactory()
               .availableFrequencies(clientInput, spectrumConstraint, result);
       if (assignable.isEmpty()) return createEmptySpectrumAssignment();

       // Phase 5 — find the highest valid center-frequency-aligned slot window
       return computeBestSpectrumAssignment(
               assignable,
               slotCount,
               cfgCollection.slots(GRANULARITY),   // LCM step in slots
               isFlexGrid,
               subscriber);
   }

   // computeBestSpectrumAssignment wires BaseFrequency + AssignSpectrumHighToLow
   public SpectrumAssignment computeBestSpectrumAssignment(
           BitSet spectrumOccupation, int slotWidth,
           int centerFrequencyGranularitySlots, boolean isFlexGrid, ...) {

       int baseIndex = new BaseFrequency().referenceFrequencySpectrumIndex(
               CENTRAL_FREQUENCY_THZ,      // 193.1 THz
               START_EDGE_FREQUENCY_THZ,   // 191.325 THz
               GRANULARITY);               // 6.25 GHz → baseIndex = 284

       Range range = new AssignSpectrumHighToLow(new SpectrumIndex()).range(
               EFFECTIVE_BITS,             // 768
               baseIndex,                  // 284 (= 193.1 THz on the grid)
               spectrumOccupation,
               centerFrequencyGranularitySlots,
               slotWidth);

       return new SpectrumAssignmentBuilder()
               .setBeginIndex(range.lower())
               .setStopIndex(range.upper())
               .setFlexGrid(isFlexGrid)
               .build();
   }


Grid constants (GridConstant)
------------------------------

The constants referenced throughout:

.. list-table::
   :header-rows: 1
   :widths: 40 20 40

   * - Constant
     - Value
     - Meaning
   * - ``EFFECTIVE_BITS``
     - 768
     - Number of 6.25 GHz slots in the C-band grid
   * - ``GRANULARITY``
     - 6.25 GHz
     - Slot width
   * - ``START_EDGE_FREQUENCY_THZ``
     - 191.325 THz
     - Lowest slot left edge (slot 0)
   * - ``CENTRAL_FREQUENCY_THZ``
     - 193.1 THz
     - ITU-T G.694.1 reference frequency (baseIndex = 284)
   * - ``NB_OCTECTS``
     - 96
     - Bytes in the 768-bit frequency map (768 / 8)
   * - ``AVAILABLE_SLOT_VALUE``
     - 0xFF
     - All bits set = all slots available
