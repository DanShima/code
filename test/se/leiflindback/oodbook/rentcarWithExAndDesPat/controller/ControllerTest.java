/*
 * Copyright (c) 2016, Leif Lindbäck
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package se.leiflindback.oodbook.rentcarWithExAndDesPat.controller;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import se.leiflindback.oodbook.rentcarWithExAndDesPat.integration.CarDTO;
import se.leiflindback.oodbook.rentcarWithExAndDesPat.integration.CarRegistryException;
import se.leiflindback.oodbook.rentcarWithExAndDesPat.integration.Printer;
import se.leiflindback.oodbook.rentcarWithExAndDesPat.model.AddressDTO;
import se.leiflindback.oodbook.rentcarWithExAndDesPat.integration.RegistryCreator;
import se.leiflindback.oodbook.rentcarWithExAndDesPat.model.AlreadyBookedException;
import se.leiflindback.oodbook.rentcarWithExAndDesPat.model.Amount;
import se.leiflindback.oodbook.rentcarWithExAndDesPat.model.CashPayment;
import se.leiflindback.oodbook.rentcarWithExAndDesPat.model.CustomerDTO;
import se.leiflindback.oodbook.rentcarWithExAndDesPat.model.DrivingLicenseDTO;
import se.leiflindback.oodbook.rentcarWithExAndDesPat.model.Rental;
import se.leiflindback.oodbook.rentcarWithExAndDesPat.model.RentalObserver;

public class ControllerTest {
    private Controller instance;
    private RegistryCreator regCreator;
    ByteArrayOutputStream outContent;
    PrintStream originalSysOut;
    private Map<CarDTO.CarType, Integer> noOfRentedCars;

    @BeforeClass
    public static void setDefaultMatcher() {
        System.setProperty("se.leiflindback.rentcar.matcher.classname",
                           "se.leiflindback.oodbook.rentcarWithExAndDesPat.integration.matching.WildCardMatch");
    }

    @Before
    public void setUp() {
        noOfRentedCars = new HashMap<>();
        for (CarDTO.CarType type : CarDTO.CarType.values()) {
            noOfRentedCars.put(type, 0);
        }

        originalSysOut = System.out;
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        Printer printer = new Printer();
        regCreator = new RegistryCreator();
        instance = new Controller(regCreator, printer);
    }

    @After
    public void tearDown() {
        outContent = null;
        System.setOut(originalSysOut);
        instance = null;
        regCreator = null;
    }

    @Test
    public void testFindAvailableCarMatch() {
        CarDTO searchedCar = new CarDTO("abc123", new Amount(1000), CarDTO.CarType.MEDIUM,
                                        true, true, "red", false);
        CarDTO expResult = searchedCar;
        CarDTO result = instance.searchMatchingCar(searchedCar);
        assertEquals("Available car was not found", expResult, result);
    }

    @Test
    public void testFindAvailableCarRegNoNotChecked() {
        CarDTO searchedCar = new CarDTO("wrong", new Amount(1000), CarDTO.CarType.MEDIUM,
                                        true, true, "red", false);
        CarDTO expResult = new CarDTO("abc123", searchedCar.getPrice(),
                                      searchedCar.getSize(), searchedCar.isAC(),
                                      searchedCar.isFourWD(), searchedCar.
                                      getColor(), false);
        CarDTO result = instance.searchMatchingCar(searchedCar);
        assertEquals("Available car with wrong regNo was not found", expResult,
                     result);
    }

    @Test
    public void testFindAvailableCarNoMatch() {
        CarDTO searchedCar = new CarDTO("abc123", new Amount(1000), CarDTO.CarType.MEDIUM,
                                        true, true, "wrong", false);
        CarDTO expResult = null;
        CarDTO result = instance.searchMatchingCar(searchedCar);
        assertEquals("Unavailable car was found", expResult, result);
    }

    @Test
    public void testFindAvailableCarNullIsIgnored() {
        CarDTO searchedCar = new CarDTO(null, null, null,
                                        true, true, null, false);
        CarDTO expResult = new CarDTO("abc123", new Amount(1000), CarDTO.CarType.MEDIUM,
                                      true, true, "red", false);
        CarDTO result = instance.searchMatchingCar(searchedCar);
        assertEquals("Unavailable car was found", expResult, result);
    }

    @Test
    public void testBookedCarIsUnavailable() {
        CarDTO bookedCar = new CarDTO("abc123", new Amount(1000), CarDTO.CarType.MEDIUM,
                                      true, true, "red", false);
        instance.registerCustomer(null);
        try {
            instance.bookCar(bookedCar);
        } catch (Exception ex) {
            fail("Got exception.");
            ex.printStackTrace();
        }
        CarDTO result = instance.searchMatchingCar(bookedCar);
        assertNull("Booked car was found", result);
    }

    @Test
    public void testBookCarThatIsBooked() {
        CarDTO bookedCar = new CarDTO("abc123", new Amount(1000), CarDTO.CarType.MEDIUM,
                                      true, true, "red", false);
        instance.registerCustomer(null);
        try {
            instance.bookCar(bookedCar);
        } catch (OperationFailedException | AlreadyBookedException ex) {
            fail("Got exception.");
            ex.printStackTrace();
        }
        try {
            instance.bookCar(bookedCar);
            fail("Managed to book a booked car.");
        } catch (OperationFailedException ex) {
            fail("Got exception.");
            ex.printStackTrace();
        } catch (AlreadyBookedException ex) {
            assertTrue("Wrong exception message, does not contain specified car: " + ex.getMessage(),
                       ex.getMessage().contains(bookedCar.getRegNo()));
            assertTrue("Wrong car is specified: " + ex.getCarThatCanNotBeBooked(),
                       ex.getCarThatCanNotBeBooked().getRegNo().equals(bookedCar.getRegNo()));
        }
    }

    @Test
    public void testBookNonexistingCar() {
        CarDTO nonexistingCar = new CarDTO("nonExisting", new Amount(1000), CarDTO.CarType.MEDIUM,
                                           true, true, "red", false);
        instance.registerCustomer(null);
        try {
            instance.bookCar(nonexistingCar);
        } catch (AlreadyBookedException ex) {
            fail("Got exception.");
            ex.printStackTrace();
        } catch (OperationFailedException ex) {
            assertTrue("Wrong root cause, expected CarRegistryException but got "
                       + ex.getCause().getClass().getCanonicalName(),
                       ex.getCause() instanceof CarRegistryException);
            assertTrue("Wrong exception message, does not contain specified car: "
                       + ex.getCause().getMessage(),
                       ex.getCause().getMessage().contains(nonexistingCar.toString()));
        }
    }

    @Test
    public void testRentalWithBookedCarIsStored() {
        String customerName = "custName";
        CustomerDTO rentingCustomer = new CustomerDTO(customerName,
                                                      new AddressDTO(
                                                              "street",
                                                              "zip",
                                                              "city"),
                                                      new DrivingLicenseDTO(
                                                              "1234567"));
        String regNo = "abc123";
        CarDTO rentedCar = new CarDTO(regNo, new Amount(1000), CarDTO.CarType.MEDIUM,
                                      true, true, "red", false);
        instance.registerCustomer(rentingCustomer);
        try {
            instance.bookCar(rentedCar);
        } catch (AlreadyBookedException | OperationFailedException ex) {
            fail("Got exception.");
            ex.printStackTrace();
        }
        List<Rental> savedRentals = regCreator.getRentalRegistry().
                findRentalByCustomerName(customerName);
        int expectedNoOfStoredRentals = 1;
        int noOfStoredRentals = savedRentals.size();
        assertEquals("Wrong number of stored rentals.",
                     expectedNoOfStoredRentals, noOfStoredRentals);
        Rental savedRental = savedRentals.get(0);
        Amount paidAmt = new Amount(5000);
        CashPayment payment = new CashPayment(paidAmt);
        savedRental.pay(payment);
        savedRental.printReceipt(new Printer());
        String result = outContent.toString();
        assertTrue("Saved rental does not contain rented car", result.contains(
                   regNo));
    }

    @Test
    public void testRegisterCustomer() {
        String customerName = "custName";
        CustomerDTO rentingCustomer = new CustomerDTO(customerName,
                                                      new AddressDTO(
                                                              "street",
                                                              "zip",
                                                              "city"),
                                                      new DrivingLicenseDTO(
                                                              "1234567"));
        String regNo = "abc123";
        CarDTO rentedCar = new CarDTO(regNo, new Amount(1000), CarDTO.CarType.MEDIUM,
                                      true, true, "red", false);
        instance.registerCustomer(rentingCustomer);
        try {
            instance.bookCar(rentedCar);
        } catch (AlreadyBookedException | OperationFailedException ex) {
            fail("Got exception.");
            ex.printStackTrace();
        }
        List<Rental> savedRentals = regCreator.getRentalRegistry().
                findRentalByCustomerName(rentingCustomer.getName());
        int expectedNoOfStoredRentals = 1;
        int noOfStoredRentals = savedRentals.size();
        assertEquals("Wrong number of stored rentals.",
                     expectedNoOfStoredRentals, noOfStoredRentals);
        Rental savedRental = savedRentals.get(0);
        String expCustName = customerName;
        assertEquals("Saved rental does not contain renting customer",
                     expCustName, savedRental.getRentingCustomer().getName());
    }

    @Test
    public void testPay() {
        Amount price = new Amount(1000);
        String regNo = "abc123";
        CarDTO.CarType size = CarDTO.CarType.MEDIUM;
        boolean AC = true;
        boolean fourWD = true;
        String color = "red";
        CarDTO rentedCar = new CarDTO(regNo, price, size, AC, fourWD, color, false);
        Amount paidAmt = new Amount(5000);
        instance.registerCustomer(null);
        try {
            instance.bookCar(rentedCar);
        } catch (AlreadyBookedException | OperationFailedException ex) {
            fail("Got exception.");
            ex.printStackTrace();
        }
        instance.pay(paidAmt);
        LocalDateTime rentalTime = LocalDateTime.now();
        String expResult = "\n\nRented car: " + regNo + "\nCost: " + price
                           + "\nChange: " + paidAmt.minus(price) + "\n\n\n";
        String result = outContent.toString();
        assertTrue("Wrong printout.", result.contains(expResult));
        assertTrue("Wrong rental year.", result.contains(Integer.toString(rentalTime.getYear())));
        assertTrue("Wrong rental month.", result.contains(Integer.toString(rentalTime.getMonthValue())));
        assertTrue("Wrong rental day.", result.contains(Integer.toString(rentalTime.getDayOfMonth())));
        assertTrue("Wrong rental hour.", result.contains(Integer.toString(rentalTime.getHour())));
        assertTrue("Wrong rental minute.", result.contains(Integer.toString(rentalTime.getMinute())));
    }

    @Test(expected = IllegalStateException.class)
    public void testBookCarWithoutCallToRegisterCustomer() throws AlreadyBookedException,
                                                                  OperationFailedException {
        instance.bookCar(null);
    }

    @Test(expected = IllegalStateException.class)
    public void testPayWithoutCallToRegisterCustomer() {
        instance.pay(null);
    }

    @Test(expected = IllegalStateException.class)
    public void testPayWithoutCallToBookCar() throws AlreadyBookedException,
                                                     OperationFailedException {
        instance.bookCar(null);
        instance.pay(null);
    }

    @Test(expected = IllegalStateException.class)
    public void testCallRegisterCustomerTwice() {
        instance.registerCustomer(null);
        instance.registerCustomer(null);
    }

    @Test
    public void testObservers() {
        int noOfObservers = 3;
        for (int i = 0; i < noOfObservers; i++) {
            instance.addRentalObserver(new CountingObserver());
        }
        CarDTO rentedCar = new CarDTO("abc123", new Amount(1000), CarDTO.CarType.LARGE, true,
                                      true, "red", false);
        instance.registerCustomer(null);
        try {
            instance.bookCar(rentedCar);
        } catch (AlreadyBookedException | OperationFailedException ex) {
            fail("Got exception.");
            ex.printStackTrace();
        }
        instance.pay(new Amount(1000));
        Map<CarDTO.CarType, Integer> expResult = new HashMap<>();
        expResult.put(CarDTO.CarType.SMALL, 0);
        expResult.put(CarDTO.CarType.MEDIUM, 0);
        expResult.put(CarDTO.CarType.LARGE, noOfObservers);
        Map<CarDTO.CarType, Integer> result = noOfRentedCars;
        for (CarDTO.CarType type : CarDTO.CarType.values()) {
            assertEquals("Observers were not correctly notified.", expResult.get(type),
                         result.get(type));
        }
    }

    @Test
    public void testNoNotificationForOngoingRental() {
        CarDTO rentedCar = new CarDTO("abc123", new Amount(1000), CarDTO.CarType.LARGE, true,
                                      true, "red", false);
        instance.registerCustomer(null);
        instance.addRentalObserver(new CountingObserver());
        try {
            instance.bookCar(rentedCar);
        } catch (AlreadyBookedException | OperationFailedException ex) {
            fail("Got exception.");
            ex.printStackTrace();
        }
        instance.pay(new Amount(1000));
        Map<CarDTO.CarType, Integer> expResult = new HashMap<>();
        expResult.put(CarDTO.CarType.SMALL, 0);
        expResult.put(CarDTO.CarType.MEDIUM, 0);
        expResult.put(CarDTO.CarType.LARGE, 0);
        Map<CarDTO.CarType, Integer> result = noOfRentedCars;
        for (CarDTO.CarType type : CarDTO.CarType.values()) {
            assertEquals("Observers were not correctly notified.", expResult.get(type),
                         result.get(type));
        }
    }

    private class CountingObserver implements RentalObserver {
        @Override
        public void newRental(CarDTO rentedCar) {
            int noOfRentedCarsOfThisType = noOfRentedCars.get(rentedCar.getSize()) + 1;
            noOfRentedCars.put(rentedCar.getSize(), noOfRentedCarsOfThisType);
        }
    }
}
