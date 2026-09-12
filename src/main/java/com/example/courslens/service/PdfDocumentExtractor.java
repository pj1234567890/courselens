package com.example.courslens.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

@Component
class PdfDocumentExtractor implements DocumentExtractor {

    private final GeminiService gemini;

    PdfDocumentExtractor(GeminiService gemini) {
        this.gemini = gemini;
    }

    @Override
    public boolean supports(String type) {
        return "PDF".equals(type);
    }

    @Override
    public List<ExtractedPage> extract(Path file, String mime) throws Exception {

        try (PDDocument pdf = Loader.loadPDF(file.toFile())) {

            List<ExtractedPage> result = new ArrayList<>();

            PDFTextStripper stripper = new PDFTextStripper();
            PDFRenderer renderer = new PDFRenderer(pdf);

            for (int pageNumber = 1;
                 pageNumber <= pdf.getNumberOfPages();
                 pageNumber++) {

                stripper.setStartPage(pageNumber);
                stripper.setEndPage(pageNumber);

                String extractedText = stripper.getText(pdf);

                if (hasMeaningfulText(extractedText)) {

                    result.add(
                            new ExtractedPage(
                                    pageNumber,
                                    extractedText.trim(),
                                    false,
                                    "PDF_PAGE"
                            )
                    );

                } else {

                    System.out.println(
                            "PDF PAGE " + pageNumber +
                                    " has no meaningful text. Using Gemini vision."
                    );

                    String visionText = extractWithVision(
                            renderer,
                            pageNumber
                    );

                    if (visionText == null || visionText.isBlank()) {
                        throw new IllegalStateException(
                                "Could not extract readable content from PDF page "
                                        + pageNumber
                        );
                    }

                    result.add(
                            new ExtractedPage(
                                    pageNumber,
                                    visionText.trim(),
                                    true,
                                    "PDF_PAGE_VISION"
                            )
                    );
                }
            }

            return result;
        }
    }

    private String extractWithVision(
            PDFRenderer renderer,
            int pageNumber) throws Exception {

        /*
         * 150 DPI is a reasonable compromise:
         * - enough resolution for handwriting
         * - avoids unnecessarily huge Gemini requests
         */
        BufferedImage image = renderer.renderImageWithDPI(
                pageNumber - 1,
                150
        );

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        ImageIO.write(image, "png", output);

        byte[] imageBytes = output.toByteArray();

        System.out.println(
                "Sending PDF page " + pageNumber +
                        " to Gemini vision. Image bytes=" +
                        imageBytes.length
        );

        return gemini.readImage(
                imageBytes,
                "image/png"
        );
    }

    private boolean hasMeaningfulText(String text) {

        if (text == null) {
            return false;
        }

        String cleaned = text
                .replaceAll("\\s+", " ")
                .trim();

        /*
         * A scanned page can sometimes contain tiny amounts of
         * accidental PDF text/metadata. Don't treat that as
         * meaningful course content.
         */
        return cleaned.length() >= 30;
    }
}