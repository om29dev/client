// src/main/java/com/mcq/client/lib/PdfUtil.java
package com.mcq.client.lib;

import org.apache.pdfbox.Loader; // <-- ADDED
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class PdfUtil {

    /**
     * Renders a single page of a PDF byte array into a BufferedImage.
     * @param pdfData The byte array of the PDF file.
     * @param pageIndex The 0-based index of the page to render.
     * @return A BufferedImage of the rendered page.
     */
    public static BufferedImage renderPage(byte[] pdfData, int pageIndex) {
        // Use 'Loader.loadPDF' instead of 'PDDocument.load'
        try (PDDocument document = Loader.loadPDF(pdfData)) { // <-- FIXED
            if (pageIndex < 0 || pageIndex >= document.getNumberOfPages()) {
                throw new IndexOutOfBoundsException("Page index is out of bounds");
            }
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            // Render at 150 DPI. Adjust as needed for quality vs. performance.
            return pdfRenderer.renderImageWithDPI(pageIndex, 150);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Renders a PDF page and scales it to fit the width of a target component.
     * @param pdfData The byte array of the PDF file.
     * @param pageIndex The 0-based index of the page to render.
     * @param targetWidth The width to scale the image to.
     * @return An ImageIcon scaled to the target width.
     */
    public static ImageIcon getScaledPdfPage(byte[] pdfData, int pageIndex, int targetWidth) {
        // Use 'Loader.loadPDF' instead of 'PDDocument.load'
        try (PDDocument document = Loader.loadPDF(pdfData)) { // <-- FIXED
            if (pageIndex < 0 || pageIndex >= document.getNumberOfPages()) {
                throw new IndexOutOfBoundsException("Page index is out of bounds");
            }
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            BufferedImage image = pdfRenderer.renderImageWithDPI(pageIndex, 150);

            if (targetWidth <= 0) targetWidth = image.getWidth();

            // Scale image to fit label while maintaining aspect ratio
            int targetHeight = (int) (((double) image.getHeight() / image.getWidth()) * targetWidth);
            Image scaledImage = image.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
            return new ImageIcon(scaledImage);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int getPageCount(byte[] pdfData) {
        // Use 'Loader.loadPDF' instead of 'PDDocument.load'
        try (PDDocument document = Loader.loadPDF(pdfData)) { // <-- FIXED
            return document.getNumberOfPages();
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }
}