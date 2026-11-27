package com.firstaidkit.unit.infrastructure;

import com.firstaidkit.controller.dto.drug.DrugFormDTO;
import com.firstaidkit.controller.dto.drug.DrugResponse;
import com.firstaidkit.infrastructure.pdf.PdfExportService;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Eksport PDF nie miał testu, który generuje plik. Ten czyta wygenerowany PDF z powrotem
 * (OpenPDF), więc łapie zarówno wyjątek przy generowaniu, jak i zgubione wiersze tabeli.
 */
class PdfExportServiceTest {

    private final PdfExportService pdfExportService = new PdfExportService();

    @Test
    void shouldWriteOneTableRowPerDrug() throws Exception {
        List<DrugResponse> drugs = List.of(
                drug(1, "Apap", DrugFormDTO.PILLS, 2026, 6, 15),
                drug(2, "Syrop", null, 2027, 1, 31)
        );

        byte[] pdf = pdfExportService.generatePdf(drugs);

        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        PdfReader reader = new PdfReader(pdf);
        try {
            assertThat(reader.getNumberOfPages()).isEqualTo(1);
            String text = new PdfTextExtractor(reader).getTextFromPage(1);
            assertThat(text)
                    .contains("First Aid Kit")
                    .contains("LP", "Name", "Form", "Expiration")
                    .contains("Apap", "PILLS", "2026-06-15")
                    .contains("Syrop", "2027-01-31");
        } finally {
            reader.close();
        }
    }

    @Test
    void shouldWriteHeaderOnlyForEmptyList() throws Exception {
        byte[] pdf = pdfExportService.generatePdf(List.of());

        PdfReader reader = new PdfReader(pdf);
        try {
            String text = new PdfTextExtractor(reader).getTextFromPage(1);
            assertThat(text).contains("Name", "Expiration").doesNotContain("PILLS");
        } finally {
            reader.close();
        }
    }

    private static DrugResponse drug(int id, String name, DrugFormDTO form, int year, int month, int day) {
        return DrugResponse.builder()
                .drugId(id)
                .drugName(name)
                .drugForm(form)
                .expirationDate(OffsetDateTime.of(year, month, day, 0, 0, 0, 0, ZoneOffset.UTC))
                .build();
    }
}
