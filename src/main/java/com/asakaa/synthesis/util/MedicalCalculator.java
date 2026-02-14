package com.asakaa.synthesis.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;

@Component
public class MedicalCalculator {

    /**
     * Calculate pediatric dose using Clark's rule
     * @param adultDoseMg Adult dose in milligrams
     * @param weightKg Patient weight in kilograms
     * @return Pediatric dose in milligrams
     */
    public double calculatePediatricDose(double adultDoseMg, double weightKg) {
        return (weightKg / 70.0) * adultDoseMg;
    }

    /**
     * Adjust dose for renal function
     * @param doseMg Original dose in milligrams
     * @param renalNormal True if renal function is normal
     * @return Adjusted dose in milligrams
     */
    public double adjustForRenalFunction(double doseMg, boolean renalNormal) {
        if (renalNormal) {
            return doseMg;
        }
        // Reduce dose by 25% for impaired renal function
        return doseMg * 0.75;
    }

    /**
     * Estimate age from date of birth
     * @param dateOfBirth Patient's date of birth
     * @return Age in years
     */
    public int estimateAge(LocalDate dateOfBirth) {
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}
