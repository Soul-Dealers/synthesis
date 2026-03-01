package com.asakaa.synthesis.integration.bedrock;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BedrockPromptBuilderTest {

    private final BedrockPromptBuilder promptBuilder = new BedrockPromptBuilder();

    @Test
    void buildDiagnosticPrompt_includesPatientHistoryWhenProvided() {
        String historyText = """
                PATIENT MEDICAL HISTORY (Last 6 months):
                Total: 3 consultations, 2 diagnoses, 1 lab results, 1 imaging findings

                CHRONOLOGICAL TIMELINE:
                • [Oct 15, 2025 09:00] CONSULTATION: Consultation: Persistent cough
                • [Oct 15, 2025 09:00] DIAGNOSIS: Diagnosis: Upper Respiratory Infection (confidence: 85%)
                • [Nov 20, 2025 10:30] CONSULTATION: Consultation: Follow-up cough
                • [Nov 20, 2025 10:30] LAB_RESULT: Lab: WBC = 11.5 10^9/L [ABNORMAL]
                • [Dec 05, 2025 14:00] IMAGING: Imaging: Chest X-ray - No consolidation""";

        ClinicalContext context = ClinicalContext.builder()
                .patientSummary("Age: 45, Gender: Male, Blood Group: O+, Allergies: Penicillin")
                .chiefComplaint("Worsening cough with blood-tinged sputum")
                .vitals("{\"temperature\": 37.8, \"bp\": \"130/85\"}")
                .patientHistory(historyText)
                .labResults("None provided")
                .imagingFindings("No previous imaging")
                .availableEquipment("Stethoscope, Thermometer")
                .localFormulary("Paracetamol, Amoxicillin")
                .build();

        String prompt = promptBuilder.buildDiagnosticPrompt(context);

        // Verify patient history is in the prompt
        assertThat(prompt).contains("PATIENT MEDICAL HISTORY");
        assertThat(prompt).contains("Upper Respiratory Infection");
        assertThat(prompt).contains("WBC = 11.5");
        assertThat(prompt).contains("Chest X-ray");
        assertThat(prompt).contains("CHRONOLOGICAL TIMELINE");

        // Verify history appears BEFORE the chief complaint
        int historyIndex = prompt.indexOf("PATIENT MEDICAL HISTORY (PAST 6 MONTHS)");
        int chiefComplaintIndex = prompt.indexOf("CHIEF COMPLAINT");
        assertThat(historyIndex).isLessThan(chiefComplaintIndex);

        // Verify the instruction to consider history is present
        assertThat(prompt).contains("Consider the patient's medical history");
    }

    @Test
    void buildDiagnosticPrompt_handlesNullPatientHistory() {
        ClinicalContext context = ClinicalContext.builder()
                .patientSummary("Age: 30, Gender: Female")
                .chiefComplaint("Headache")
                .vitals("Normal")
                .patientHistory(null)
                .build();

        String prompt = promptBuilder.buildDiagnosticPrompt(context);

        assertThat(prompt).contains("No consultation history available for the past 6 months.");
    }

    @Test
    void buildDiagnosticPrompt_handlesEmptyPatientHistory() {
        ClinicalContext context = ClinicalContext.builder()
                .patientSummary("Age: 30, Gender: Female")
                .chiefComplaint("Headache")
                .vitals("Normal")
                .patientHistory("No consultation history available for the past 6 months.")
                .build();

        String prompt = promptBuilder.buildDiagnosticPrompt(context);

        assertThat(prompt).contains("No consultation history available for the past 6 months.");
    }
}
