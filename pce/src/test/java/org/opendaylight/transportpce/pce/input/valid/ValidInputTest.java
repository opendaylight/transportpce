/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.input.valid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.answer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer2;
import org.mockito.stubbing.Answer3;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.ServiceAEnd1;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.ServiceAEnd1Builder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.ServiceZEnd1;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.ServiceZEnd1Builder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.SlotWidthFrequencyGHz;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.spectrum.allocation.FrequencySlot;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.spectrum.allocation.FrequencySlotBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev250110.FrequencyTHz;

class ValidInputTest {

    @Test
    void assertEmptyPCRIisValid() {
        ValidInput validateInput = new ValidInput(mock(Slot.class), mock(Format.class));
        assertTrue(validateInput.isValid(new PathComputationRequestInputBuilder().build()));
    }

    @Test
    void isValidZEndSlot() {
        FrequencyTHz centerFrequency = FrequencyTHz.getDefaultInstance("191.33125");
        SlotWidthFrequencyGHz slotWidthFrequencyGHz = SlotWidthFrequencyGHz.getDefaultInstance("50");
        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setCenterFrequency(centerFrequency)
                .setSlotWidth(slotWidthFrequencyGHz)
                .build();
        ServiceZEnd1 serviceZEnd1 = new ServiceZEnd1Builder()
                .setFrequencySlot(frequencySlot)
                .build();
        ServiceZEnd serviceZEnd = new ServiceZEndBuilder()
                .addAugmentation(serviceZEnd1)
                .build();
        PathComputationRequestInputBuilder pathComputationRequestInputBuilder =
                new PathComputationRequestInputBuilder();
        pathComputationRequestInputBuilder.setServiceZEnd(serviceZEnd);
        Slot mock = mock(Slot.class);
        Format formatMock = mock(Format.class);
        when(formatMock.isValidFormat(any(), any(), any())).thenReturn(true);
        ValidInput validateInput = new ValidInput(mock, formatMock);
        when(mock.isValidSlot(
                centerFrequency.getValue().decimalValue(),
                slotWidthFrequencyGHz.getValue().decimalValue(),
                validateInput
        )).thenReturn(true);

        assertTrue(validateInput.isValid(pathComputationRequestInputBuilder.build()));

        verify(mock).isValidSlot(
                centerFrequency.getValue().decimalValue(),
                slotWidthFrequencyGHz.getValue().decimalValue(),
                validateInput
        );
        verify(mock, never()).isValidCenterFrequency(any(), any());
        verify(mock, never()).isValidSlotWidth(any(), any());
    }

    @Test
    void invalidZEndSlot_callsIsValidSlotAndReturnsFalse() {
        FrequencyTHz centerFrequency = FrequencyTHz.getDefaultInstance("191.33125");
        SlotWidthFrequencyGHz slotWidth = SlotWidthFrequencyGHz.getDefaultInstance("50");

        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setCenterFrequency(centerFrequency)
                .setSlotWidth(slotWidth)
                .build();

        ServiceZEnd serviceZEnd = new ServiceZEndBuilder()
                .addAugmentation(new ServiceZEnd1Builder()
                        .setFrequencySlot(frequencySlot)
                        .build())
                .build();

        PathComputationRequestInput input = new PathComputationRequestInputBuilder()
                .setServiceZEnd(serviceZEnd)
                .build();

        Slot slotMock = mock(Slot.class);
        Format formatMock = mock(Format.class);

        // Format must pass, otherwise slot is never called
        when(formatMock.isValidFormat(any(), any(), any())).thenReturn(true);

        ValidInput validateInput = new ValidInput(slotMock, formatMock);

        BigDecimal center = centerFrequency.getValue().decimalValue();
        BigDecimal width = slotWidth.getValue().decimalValue();

        when(slotMock.isValidSlot(center, width, validateInput))
                .thenReturn(false);

        assertFalse(validateInput.isValid(input));

        verify(slotMock).isValidSlot(center, width, validateInput);

        verify(slotMock, never()).isValidCenterFrequency(any(), any());
        verify(slotMock, never()).isValidSlotWidth(any(), any());
    }

    @Test
    void isValidAEndSlot() {
        FrequencyTHz centerFrequency = FrequencyTHz.getDefaultInstance("191.33125");
        SlotWidthFrequencyGHz slotWidthFrequencyGHz = SlotWidthFrequencyGHz.getDefaultInstance("50");
        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setCenterFrequency(centerFrequency)
                .setSlotWidth(slotWidthFrequencyGHz)
                .build();
        ServiceAEnd1 serviceAEnd1 = new ServiceAEnd1Builder()
                .setFrequencySlot(frequencySlot)
                .build();
        ServiceAEnd serviceAEnd = new ServiceAEndBuilder()
                .addAugmentation(serviceAEnd1)
                .build();
        PathComputationRequestInputBuilder pathComputationRequestInputBuilder =
                new PathComputationRequestInputBuilder();
        pathComputationRequestInputBuilder.setServiceAEnd(serviceAEnd);
        Slot mock = mock(Slot.class);
        Format formatMock = mock(Format.class);
        when(formatMock.isValidFormat(any(), any(), any())).thenReturn(true);
        ValidInput validateInput = new ValidInput(mock, formatMock);
        when(mock.isValidSlot(
                centerFrequency.getValue().decimalValue(),
                slotWidthFrequencyGHz.getValue().decimalValue(),
                validateInput
        )).thenReturn(true);

        assertTrue(validateInput.isValid(pathComputationRequestInputBuilder.build()));

        verify(mock).isValidSlot(
                centerFrequency.getValue().decimalValue(),
                slotWidthFrequencyGHz.getValue().decimalValue(),
                validateInput
        );
        verify(mock, never()).isValidCenterFrequency(any(), any());
        verify(mock, never()).isValidSlotWidth(any(), any());
    }

    @Test
    void invalidAEndSlot_callsIsValidSlotAndReturnsFalse() {
        FrequencyTHz centerFrequency = FrequencyTHz.getDefaultInstance("191.33125");
        SlotWidthFrequencyGHz slotWidth = SlotWidthFrequencyGHz.getDefaultInstance("50");

        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setCenterFrequency(centerFrequency)
                .setSlotWidth(slotWidth)
                .build();

        ServiceAEnd serviceAEnd = new ServiceAEndBuilder()
                .addAugmentation(new ServiceAEnd1Builder()
                        .setFrequencySlot(frequencySlot)
                        .build())
                .build();

        PathComputationRequestInput input = new PathComputationRequestInputBuilder()
                .setServiceAEnd(serviceAEnd)
                .build();

        Slot slotMock = mock(Slot.class);
        Format formatMock = mock(Format.class);
        when(formatMock.isValidFormat(any(), any(), any())).thenReturn(true);

        ValidInput validateInput = new ValidInput(slotMock, formatMock);

        BigDecimal center = centerFrequency.getValue().decimalValue();
        BigDecimal width = slotWidth.getValue().decimalValue();

        when(slotMock.isValidSlot(center, width, validateInput)).thenReturn(false);

        assertFalse(validateInput.isValid(input));

        verify(slotMock).isValidSlot(center, width, validateInput);
        verify(slotMock, never()).isValidCenterFrequency(any(), any());
        verify(slotMock, never()).isValidSlotWidth(any(), any());
    }

    @Test
    void invalidAEndCenterFrequency_callsIsValidCenterFrequencyAndReturnsFalse() {
        FrequencyTHz centerFrequency = FrequencyTHz.getDefaultInstance("191.33125");

        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setCenterFrequency(centerFrequency)
                .build();

        ServiceAEnd serviceAEnd = new ServiceAEndBuilder()
                .addAugmentation(new ServiceAEnd1Builder()
                        .setFrequencySlot(frequencySlot)
                        .build())
                .build();

        PathComputationRequestInput input = new PathComputationRequestInputBuilder()
                .setServiceAEnd(serviceAEnd)
                .build();

        Slot slotMock = mock(Slot.class);
        Format formatMock = mock(Format.class);
        when(formatMock.isValidFormat(any(), any(), any())).thenReturn(true);

        ValidInput validateInput = new ValidInput(slotMock, formatMock);

        BigDecimal center = centerFrequency.getValue().decimalValue();

        when(slotMock.isValidCenterFrequency(center, validateInput)).thenReturn(false);

        assertFalse(validateInput.isValid(input));

        verify(slotMock).isValidCenterFrequency(center, validateInput);
        verify(slotMock, never()).isValidSlot(any(), any(), any());
        verify(slotMock, never()).isValidSlotWidth(any(), any());
    }

    @Test
    void invalidZEndCenterFrequency_callsIsValidCenterFrequencyAndReturnsFalse() {
        FrequencyTHz centerFrequency = FrequencyTHz.getDefaultInstance("191.33125");

        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setCenterFrequency(centerFrequency)
                .build();

        ServiceZEnd serviceZEnd = new ServiceZEndBuilder()
                .addAugmentation(new ServiceZEnd1Builder()
                        .setFrequencySlot(frequencySlot)
                        .build())
                .build();

        PathComputationRequestInput input = new PathComputationRequestInputBuilder()
                .setServiceZEnd(serviceZEnd)
                .build();

        Slot slotMock = mock(Slot.class);
        Format formatMock = mock(Format.class);
        when(formatMock.isValidFormat(any(), any(), any())).thenReturn(true);

        ValidInput validateInput = new ValidInput(slotMock, formatMock);

        BigDecimal center = centerFrequency.getValue().decimalValue();

        when(slotMock.isValidCenterFrequency(center, validateInput)).thenReturn(false);

        assertFalse(validateInput.isValid(input));

        verify(slotMock).isValidCenterFrequency(center, validateInput);
        verify(slotMock, never()).isValidSlot(any(), any(), any());
        verify(slotMock, never()).isValidSlotWidth(any(), any());
    }

    @Test
    void invalidAEndSlotWidth_callsIsValidSlotWidthAndReturnsFalse() {
        SlotWidthFrequencyGHz slotWidth = SlotWidthFrequencyGHz.getDefaultInstance("50");

        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setSlotWidth(slotWidth)
                .build();

        ServiceAEnd serviceAEnd = new ServiceAEndBuilder()
                .addAugmentation(new ServiceAEnd1Builder()
                        .setFrequencySlot(frequencySlot)
                        .build())
                .build();

        PathComputationRequestInput input = new PathComputationRequestInputBuilder()
                .setServiceAEnd(serviceAEnd)
                .build();

        Slot slotMock = mock(Slot.class);
        Format formatMock = mock(Format.class);
        when(formatMock.isValidFormat(any(), any(), any())).thenReturn(true);

        ValidInput validateInput = new ValidInput(slotMock, formatMock);

        BigDecimal width = slotWidth.getValue().decimalValue();

        when(slotMock.isValidSlotWidth(width, validateInput)).thenReturn(false);

        assertFalse(validateInput.isValid(input));

        verify(slotMock).isValidSlotWidth(width, validateInput);
        verify(slotMock, never()).isValidCenterFrequency(any(), any());
        verify(slotMock, never()).isValidSlot(any(), any(), any());
    }

    /**
     * The intention of this test is to ensure any error message produced by
     * the method isValidSlot (defined in interface Slot) is passed upstream
     * in the call stack.
     */
    @Test
    void assertSlotErrorMessageIsPassedUpTheCallStack() {
        FrequencyTHz centerFrequency = FrequencyTHz.getDefaultInstance("191.33125");
        SlotWidthFrequencyGHz slotWidthFrequencyGHz = SlotWidthFrequencyGHz.getDefaultInstance("50");
        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setCenterFrequency(centerFrequency)
                .setSlotWidth(slotWidthFrequencyGHz)
                .build();
        ServiceZEnd1 serviceZEnd1 = new ServiceZEnd1Builder()
                .setFrequencySlot(frequencySlot)
                .build();
        ServiceZEnd serviceZEnd = new ServiceZEndBuilder()
                .addAugmentation(serviceZEnd1)
                .build();
        PathComputationRequestInputBuilder pathComputationRequestInputBuilder =
                new PathComputationRequestInputBuilder();
        pathComputationRequestInputBuilder.setServiceZEnd(serviceZEnd);
        Slot slot = mock(Slot.class);
        Format formatMock = mock(Format.class);
        when(formatMock.isValidFormat(any(), any(), any())).thenReturn(true);

        //validateInput is an observer that should receive any error message
        //from the instance of Slot. We'll mock an instance of Slot further down below.
        ValidInput validateInput = new ValidInput(slot, formatMock);

        //This mock will simulate a failed slot width validation and pass an error
        //message to the observer (argument1, i.e. validateInput).
        doAnswer(answer(new Answer3<Boolean, BigDecimal, BigDecimal, Observer>() {
            @Override
            public Boolean answer(BigDecimal argument0, BigDecimal argument1, Observer argument2) throws Throwable {
                argument2.error("An error occurred");
                return false;
            }
        })).when(slot).isValidSlot(
                centerFrequency.getValue().decimalValue(),
                slotWidthFrequencyGHz.getValue().decimalValue(),
                validateInput);

        assertFalse(validateInput.isValid(pathComputationRequestInputBuilder.build()));

        //Assert the error message is available in the observer object.
        assertEquals("An error occurred", validateInput.lastErrorMessage());

        verify(slot).isValidSlot(
                centerFrequency.getValue().decimalValue(),
                slotWidthFrequencyGHz.getValue().decimalValue(),
                validateInput
        );
        verify(slot, never()).isValidCenterFrequency(any(), any());
        verify(slot, never()).isValidSlotWidth(any(), any());
    }

    /**
     * The intention of this test is to ensure any error message produced by
     * the method isValidCenterFrequency (defined in interface Slot) is passed upstream
     * in the call stack.
     */
    @Test
    void assertCenterFrequencyErrorMessageIsPassedUpTheCallStack() {
        FrequencyTHz centerFrequency = FrequencyTHz.getDefaultInstance("191.33125");
        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setCenterFrequency(centerFrequency)
                .build();
        ServiceZEnd1 serviceZEnd1 = new ServiceZEnd1Builder()
                .setFrequencySlot(frequencySlot)
                .build();
        ServiceZEnd serviceZEnd = new ServiceZEndBuilder()
                .addAugmentation(serviceZEnd1)
                .build();
        PathComputationRequestInputBuilder pathComputationRequestInputBuilder =
                new PathComputationRequestInputBuilder();
        pathComputationRequestInputBuilder.setServiceZEnd(serviceZEnd);
        Slot slot = mock(Slot.class);
        Format formatMock = mock(Format.class);
        when(formatMock.isValidFormat(any(), any(), any())).thenReturn(true);

        //validateInput is an observer that should receive any error message
        //from the instance of Slot. We'll mock an instance of Slot further down below.
        ValidInput validateInput = new ValidInput(slot, formatMock);

        //This mock will simulate a failed slot width validation and pass an error
        //message to the observer (argument1, i.e. validateInput).
        doAnswer(answer(new Answer2<Boolean, BigDecimal, Observer>() {
            @Override
            public Boolean answer(BigDecimal argument0, Observer argument1) throws Throwable {
                argument1.error("An error occurred");
                return false;
            }
        })).when(slot).isValidCenterFrequency(centerFrequency.getValue().decimalValue(), validateInput);

        assertFalse(validateInput.isValid(pathComputationRequestInputBuilder.build()));

        //Assert the error message is available in the observer object.
        assertEquals("An error occurred", validateInput.lastErrorMessage());
    }

    /**
     * The intention of this test is to ensure any error message produced by
     * the method isValidSlotWidth (defined in interface Slot) is passed upstream
     * in the call stack.
     */
    @Test
    void assertSlotWidthErrorMessageIsPassedUpTheCallStack() {
        SlotWidthFrequencyGHz slotWidthFrequencyGHz = SlotWidthFrequencyGHz.getDefaultInstance("50");
        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setSlotWidth(slotWidthFrequencyGHz)
                .build();
        ServiceZEnd1 serviceZEnd1 = new ServiceZEnd1Builder()
                .setFrequencySlot(frequencySlot)
                .build();
        ServiceZEnd serviceZEnd = new ServiceZEndBuilder()
                .addAugmentation(serviceZEnd1)
                .build();
        PathComputationRequestInputBuilder pathComputationRequestInputBuilder =
                new PathComputationRequestInputBuilder();
        pathComputationRequestInputBuilder.setServiceZEnd(serviceZEnd);
        Slot slot = mock(Slot.class);
        Format formatMock = mock(Format.class);
        when(formatMock.isValidFormat(any(), any(), any())).thenReturn(true);

        //validateInput is an observer that should receive any error message
        //from the instance of Slot. We'll mock an instance of Slot further down below.
        ValidInput validateInput = new ValidInput(slot, formatMock);

        //This mock will simulate a failed slot width validation and pass an error
        //message to the observer (argument1, i.e. validateInput).
        doAnswer(answer(new Answer2<Boolean, BigDecimal, Observer>() {
            @Override
            public Boolean answer(BigDecimal argument0, Observer argument1) throws Throwable {
                argument1.error("An error occurred");
                return false;
            }
        })).when(slot).isValidSlotWidth(slotWidthFrequencyGHz.getValue().decimalValue(), validateInput);

        assertFalse(validateInput.isValid(pathComputationRequestInputBuilder.build()));

        //Assert the error message is available in the observer object.
        assertEquals("An error occurred", validateInput.lastErrorMessage());
    }

    @Test
    void assertFormatErrorMessageIsPassedUpTheCallStack() {
        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setSlotWidth(SlotWidthFrequencyGHz.getDefaultInstance("50"))
                .build();
        ServiceZEnd1 serviceZEnd1 = new ServiceZEnd1Builder()
                .setFrequencySlot(frequencySlot)
                .build();
        ServiceZEnd serviceZEnd = new ServiceZEndBuilder()
                .addAugmentation(serviceZEnd1)
                .build();
        PathComputationRequestInputBuilder pathComputationRequestInputBuilder =
                new PathComputationRequestInputBuilder();
        pathComputationRequestInputBuilder.setServiceZEnd(serviceZEnd);

        Slot slot = mock(Slot.class);
        when(slot.isValidSlotWidth(any(), any())).thenReturn(true);
        Format formatMock = mock(Format.class);

        //validateInput is an observer that should receive any error message
        //from the instance of Slot. We'll mock an instance of Slot further down below.
        ValidInput validateInput = new ValidInput(slot, formatMock);

        PathComputationRequestInput pcri = pathComputationRequestInputBuilder.build();

        //This mock will simulate a failed slot width validation and pass an error
        //message to the observer (argument1, i.e. validateInput).
        doAnswer(answer(new Answer3<Boolean, String, FrequencySlot, Observer>() {
            @Override
            public Boolean answer(String arg0, FrequencySlot arg2, Observer arg3) throws Throwable {
                arg3.error("An error occurred");
                return false;
            }
        })).when(formatMock).isValidFormat(null, frequencySlot, validateInput);

        assertFalse(validateInput.isValid(pcri));

        //Assert the error message is available in the observer object.
        assertEquals("An error occurred", validateInput.lastErrorMessage());

        verify(formatMock).isValidFormat(null, frequencySlot, validateInput);
        verify(slot, never()).isValidSlot(any(), any(), any());
        verify(slot, never()).isValidCenterFrequency(any(), any());
        verify(slot, never()).isValidSlotWidth(any(), any());
    }

    /**
     * The intention of this test is to ensure any error message produced by
     * the method valid is reset on subsequent calls.
     */
    @Test
    void assertSubsequentValidationsResetLastErrorMessage() {
        //Instantiate a Slot mock. We'll wire up slot to respond differently depending on input further down below.
        Slot slot = mock(Slot.class);
        Format formatMock = mock(Format.class);
        when(formatMock.isValidFormat(any(), any(), any())).thenReturn(true);

        //validateInput is an observer that should receive any error message
        //from the instance of Slot.
        ValidInput validateInput = new ValidInput(slot, formatMock);
        SlotWidthFrequencyGHz slotWidthFrequencyGHz = SlotWidthFrequencyGHz.getDefaultInstance("50.0");
        //This mocked response will simulate a failed slot width validation and pass an error
        //message to the observer (argument1, i.e. validateInput). Note the value '50.0'.
        doAnswer(answer(new Answer2<Boolean, BigDecimal, Observer>() {
            @Override
            public Boolean answer(BigDecimal argument0, Observer argument1) throws Throwable {
                argument1.error("An error occurred");
                return false;
            }
        })).when(slot).isValidSlotWidth(slotWidthFrequencyGHz.getValue().decimalValue(), validateInput);

        // This input matches the mocked response above, meaning this input together with the mock is set up to
        // produce an input validation error. Note the value '50'.

        PathComputationRequestInput pathComputationRequestInput1 = new PathComputationRequestInputBuilder()
                .setServiceZEnd(new ServiceZEndBuilder()
                        .addAugmentation(new ServiceZEnd1Builder()
                                .setFrequencySlot(new FrequencySlotBuilder()
                                        .setSlotWidth(slotWidthFrequencyGHz)
                                        .build())
                                .build())
                        .build())
                .build();
        FrequencyTHz centerFrequency = FrequencyTHz.getDefaultInstance("191.33125");
        //This mocked response will simulate a successful validation, i.e. no error message, note the value '191.33125'.
        doAnswer(answer(new Answer2<Boolean, BigDecimal, Observer>() {
            @Override
            public Boolean answer(BigDecimal argument0, Observer argument1) throws Throwable {
                return true;
            }
        })).when(slot).isValidCenterFrequency(centerFrequency.getValue().decimalValue(), validateInput);

        // Build a second input. Unlike pathComputationRequestInput1, this is set up to pass validation, note the value
        // 191.33125

        PathComputationRequestInput pathComputationRequestInput2 = new PathComputationRequestInputBuilder()
                .setServiceZEnd(new ServiceZEndBuilder()
                        .addAugmentation(new ServiceZEnd1Builder()
                                .setFrequencySlot(new FrequencySlotBuilder()
                                        .setCenterFrequency(centerFrequency)
                                        .build())
                                .build())
                        .build())
                .build();

        // Start testing.
        //First validation should fail...
        assertFalse(validateInput.isValid(pathComputationRequestInput1));

        //... and an error message should be available...
        assertEquals("An error occurred", validateInput.lastErrorMessage());

        //... the second validation should pass...
        assertTrue(validateInput.isValid(pathComputationRequestInput2));

        //... and since the last validation request above didn't produce an error, this error message should be empty.
        assertEquals("", validateInput.lastErrorMessage());
    }
}
