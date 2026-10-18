package com.ankit.orderservice;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Order validOrder() {
        Order o = new Order();
        o.setCustomerName("Asha");
        o.setProductName("Laptop");
        o.setQuantity(1);
        o.setPrice(new BigDecimal("100.50"));
        return o;
    }

    @Test
    void validOrderHasNoViolations() {
        assertThat(validator.validate(validOrder())).isEmpty();
    }

    @Test
    void priceWithThreeDecimalsIsRejected() {
        Order o = validOrder();
        o.setPrice(new BigDecimal("100.555"));
        assertThat(validator.validate(o)).isNotEmpty();
    }

    @Test
    void zeroAndNegativePricesAreRejected() {
        Order zero = validOrder();
        zero.setPrice(BigDecimal.ZERO);
        Order negative = validOrder();
        negative.setPrice(new BigDecimal("-5.00"));
        assertThat(validator.validate(zero)).isNotEmpty();
        assertThat(validator.validate(negative)).isNotEmpty();
    }

    @Test
    void tooLargePriceIsRejected() {
        Order o = validOrder();
        o.setPrice(new BigDecimal("100000000.00"));
        assertThat(validator.validate(o)).isNotEmpty();
    }

    @Test
    void smallestAndLargestAllowedPricesAreAccepted() {
        Order low = validOrder();
        low.setPrice(new BigDecimal("0.01"));
        Order high = validOrder();
        high.setPrice(new BigDecimal("9999999.99"));
        assertThat(validator.validate(low)).isEmpty();
        assertThat(validator.validate(high)).isEmpty();
    }

    @Test
    void quantityOutsideRangeIsRejected() {
        Order zero = validOrder();
        zero.setQuantity(0);
        Order tooMany = validOrder();
        tooMany.setQuantity(1001);
        assertThat(validator.validate(zero)).isNotEmpty();
        assertThat(validator.validate(tooMany)).isNotEmpty();
    }

    @Test
    void decimalAmountsAddUpExactly() {
        BigDecimal total = new BigDecimal("0.10").multiply(BigDecimal.valueOf(1))
                .add(new BigDecimal("0.20").multiply(BigDecimal.valueOf(1)));
        assertThat(total).isEqualByComparingTo("0.30");
    }
}