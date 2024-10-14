/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.input.valid;

import static org.mockito.AdditionalAnswers.answer;
import static org.mockito.Mockito.doAnswer;

import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev230526.FrequencyTHz;

class ValidInputTest {

    @Test
    void assertEmptyPCRIisValid() {
        ValidInput validateInput = new ValidInput(Mockito.mock(Slot.class), Mockito.mock(Format.class));
        Assertions.assertTrue(validateInput.isValid(Mockito.mock(PathComputationRequestInput.class)));
    }

    @Test
    void isValidZEndSlot() {

        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setCenterFrequency(FrequencyTHz.getDefaultInstance("191.33125"))
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

        Slot mock = Mockito.mock(Slot.class);

        Format formatMock = Mockito.mock(Format.class);
        Mockito.when(formatMock.isValidFormat(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);
        ValidInput validateInput = new ValidInput(mock, formatMock);
        Mockito.when(mock.isValidSlot(BigDecimal.valueOf(191.33125), BigDecimal.valueOf(50.0), validateInput))
                .thenReturn(true);

        Assertions.assertTrue(validateInput.isValid(pathComputationRequestInputBuilder.build()));

    }

    @Test
    void inValidZEndSlot() {

        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setCenterFrequency(FrequencyTHz.getDefaultInstance("191.33125"))
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

        Slot mock = Mockito.mock(Slot.class);

        ValidInput validateInput = new ValidInput(mock, Mockito.mock(Format.class));
        Mockito.when(mock.isValidSlot(BigDecimal.valueOf(191.33125), BigDecimal.valueOf(12.5), validateInput))
                .thenReturn(false);

        Assertions.assertFalse(validateInput.isValid(pathComputationRequestInputBuilder.build()));

    }

    @Test
    void isValidAEndSlot() {

        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setCenterFrequency(FrequencyTHz.getDefaultInstance("191.33125"))
                .setSlotWidth(SlotWidthFrequencyGHz.getDefaultInstance("50"))
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

        Slot mock = Mockito.mock(Slot.class);

        Format formatMock = Mockito.mock(Format.class);
        Mockito.when(formatMock.isValidFormat(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);

        ValidInput validateInput = new ValidInput(mock, formatMock);

        Mockito.when(mock.isValidSlot(BigDecimal.valueOf(191.33125), BigDecimal.valueOf(50.0), validateInput))
                .thenReturn(true);

        Assertions.assertTrue(validateInput.isValid(pathComputationRequestInputBuilder.build()));

    }

    @Test
    void inValidAEndSlot() {

        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setCenterFrequency(FrequencyTHz.getDefaultInstance("191.33125"))
                .setSlotWidth(SlotWidthFrequencyGHz.getDefaultInstance("50"))
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

        Slot mock = Mockito.mock(Slot.class);

        ValidInput validateInput = new ValidInput(mock, Mockito.mock(Format.class));
        Mockito.when(mock.isValidSlot(BigDecimal.valueOf(191.33125), BigDecimal.valueOf(12.5), validateInput))
                .thenReturn(false);

        Assertions.assertFalse(validateInput.isValid(pathComputationRequestInputBuilder.build()));

    }

    @Test
    void inValidAEndCenterFrequency() {

        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setCenterFrequency(FrequencyTHz.getDefaultInstance("191.33125"))
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

        Slot mock = Mockito.mock(Slot.class);

        ValidInput validateInput = new ValidInput(mock, Mockito.mock(Format.class));
        Mockito.when(mock.isValidCenterFrequency(BigDecimal.valueOf(191.33125), validateInput))
                .thenReturn(false);

        Assertions.assertFalse(validateInput.isValid(pathComputationRequestInputBuilder.build()));

    }

    @Test
    void inValidZEndCenterFrequency() {

        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setCenterFrequency(FrequencyTHz.getDefaultInstance("191.33125"))
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

        Slot mock = Mockito.mock(Slot.class);

        ValidInput validateInput = new ValidInput(mock, Mockito.mock(Format.class));
        Mockito.when(mock.isValidSlot(BigDecimal.valueOf(191.33125), BigDecimal.valueOf(12.5), validateInput))
                .thenReturn(false);

        Assertions.assertFalse(validateInput.isValid(pathComputationRequestInputBuilder.build()));

    }

    @Test
    void inValidAEndSlotWidth() {

        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setSlotWidth(SlotWidthFrequencyGHz.getDefaultInstance("50"))
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

        Slot mock = Mockito.mock(Slot.class);

        ValidInput validateInput = new ValidInput(mock, Mockito.mock(Format.class));
        Mockito.when(mock.isValidCenterFrequency(BigDecimal.valueOf(191.33125), validateInput))
                .thenReturn(false);

        Assertions.assertFalse(validateInput.isValid(pathComputationRequestInputBuilder.build()));

    }

    /**
     * The intention of this test is to ensure any error message produced by
     * the method isValidSlot (defined in interface Slot) is passed upstream
     * in the call stack.
     */
    @Test
    void assertSlotErrorMessageIsPassedUpTheCallStack() {

        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setCenterFrequency(FrequencyTHz.getDefaultInstance("191.33125"))
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

        Slot slot = Mockito.mock(Slot.class);
        Format formatMock = Mockito.mock(Format.class);
        Mockito.when(formatMock.isValidFormat(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);

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
        })).when(slot).isValidSlot(BigDecimal.valueOf(191.33125), BigDecimal.valueOf(50.0), validateInput);

        Assertions.assertFalse(validateInput.isValid(pathComputationRequestInputBuilder.build()));

        //Assert the error message is available in the observer object.
        Assertions.assertEquals("An error occurred", validateInput.lastErrorMessage());
    }

    /**
     * The intention of this test is to ensure any error message produced by
     * the method isValidCenterFrequency (defined in interface Slot) is passed upstream
     * in the call stack.
     */
    @Test
    void assertCenterFrequencyErrorMessageIsPassedUpTheCallStack() {

        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setCenterFrequency(FrequencyTHz.getDefaultInstance("191.33125"))
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

        Slot slot = Mockito.mock(Slot.class);
        Format formatMock = Mockito.mock(Format.class);
        Mockito.when(formatMock.isValidFormat(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);

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
        })).when(slot).isValidCenterFrequency(BigDecimal.valueOf(191.33125), validateInput);

        Assertions.assertFalse(validateInput.isValid(pathComputationRequestInputBuilder.build()));

        //Assert the error message is available in the observer object.
        Assertions.assertEquals("An error occurred", validateInput.lastErrorMessage());
    }

    /**
     * The intention of this test is to ensure any error message produced by
     * the method isValidSlotWidth (defined in interface Slot) is passed upstream
     * in the call stack.
     */
    @Test
    void assertSlotWidthErrorMessageIsPassedUpTheCallStack() {

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

        Slot slot = Mockito.mock(Slot.class);
        Format formatMock = Mockito.mock(Format.class);
        Mockito.when(formatMock.isValidFormat(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);

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
        })).when(slot).isValidSlotWidth(BigDecimal.valueOf(50.0), validateInput);

        Assertions.assertFalse(validateInput.isValid(pathComputationRequestInputBuilder.build()));

        //Assert the error message is available in the observer object.
        Assertions.assertEquals("An error occurred", validateInput.lastErrorMessage());
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

        Slot slot = Mockito.mock(Slot.class);
        Mockito.when(slot.isValidSlotWidth(Mockito.any(), Mockito.any())).thenReturn(true);

        Format formatMock = Mockito.mock(Format.class);

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

        Assertions.assertFalse(validateInput.isValid(pcri));

        //Assert the error message is available in the observer object.
        Assertions.assertEquals("An error occurred", validateInput.lastErrorMessage());
    }

    /**
     * The intention of this test is to ensure any error message produced by
     * the method valid is reset on subsequent calls.
     */
    @Test
    void assertSubsequentValidationsResetLastErrorMessage() {

        //Instantiate a Slot mock. We'll wire up slot to respond differently depending on input further down below.
        Slot slot = Mockito.mock(Slot.class);

        Format formatMock = Mockito.mock(Format.class);
        Mockito.when(formatMock.isValidFormat(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);

        //validateInput is an observer that should receive any error message
        //from the instance of Slot.
        ValidInput validateInput = new ValidInput(slot, formatMock);

        //This mocked response will simulate a failed slot width validation and pass an error
        //message to the observer (argument1, i.e. validateInput). Note the value '50.0'.
        doAnswer(answer(new Answer2<Boolean, BigDecimal, Observer>() {
            @Override
            public Boolean answer(BigDecimal argument0, Observer argument1) throws Throwable {
                argument1.error("An error occurred");
                return false;
            }
        })).when(slot).isValidSlotWidth(BigDecimal.valueOf(50.0), validateInput);

        // This input matches the mocked response above, meaning this input together with the mock is set up to
        // produce an input validation error. Note the value '50'.
        PathComputationRequestInput pathComputationRequestInput1 =
                new PathComputationRequestInputBuilder().setServiceZEnd(
                        new ServiceZEndBuilder()
                                .addAugmentation(
                                        new ServiceZEnd1Builder()
                                                .setFrequencySlot(
                                                        new FrequencySlotBuilder()
                                                                .setSlotWidth(SlotWidthFrequencyGHz
                                                                        .getDefaultInstance("50")
                                                                ).build()
                                                )
                                                .build()
                                )
                                .build()
                ).build();

        //This mocked response will simulate a successful validation, i.e. no error message, note the value '191.33125'.
        doAnswer(answer(new Answer2<Boolean, BigDecimal, Observer>() {
            @Override
            public Boolean answer(BigDecimal argument0, Observer argument1) throws Throwable {
                return true;
            }
        })).when(slot).isValidCenterFrequency(BigDecimal.valueOf(191.33125), validateInput);

        // Build a second input. Unlike pathComputationRequestInput1, this is set up to pass validation, note the value
        // 191.33125
        PathComputationRequestInput pathComputationRequestInput2 =
                new PathComputationRequestInputBuilder().setServiceZEnd(
                        new ServiceZEndBuilder()
                                .addAugmentation(
                                        new ServiceZEnd1Builder()
                                                .setFrequencySlot(
                                                        new FrequencySlotBuilder()
                                                                .setCenterFrequency(
                                                                        FrequencyTHz.getDefaultInstance("191.33125")
                                                                )
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                ).build();

        // Start testing.
        //First validation should fail...
        Assertions.assertFalse(validateInput.isValid(pathComputationRequestInput1));

        //... and an error message should be available...
        Assertions.assertEquals("An error occurred", validateInput.lastErrorMessage());

        //... the second validation should pass...
        Assertions.assertTrue(validateInput.isValid(pathComputationRequestInput2));

        //... and since the last validation request above didn't produce an error, this error message should be empty.
        Assertions.assertEquals("", validateInput.lastErrorMessage());
    }
}
