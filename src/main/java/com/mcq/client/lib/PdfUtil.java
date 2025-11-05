package com.mcq.client.lib;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class PdfUtil {

    public static BufferedImage renderPage(byte[] pdfData, int pageIndex) {
        try (PDDocument document = Loader.loadPDF(pdfData)) {
            if (pageIndex < 0 || pageIndex >= document.getNumberOfPages()) {
                throw new IndexOutOfBoundsException("Page index is out of bounds");
            }
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            return pdfRenderer.renderImageWithDPI(pageIndex, 150);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ImageIcon getScaledPdfPage(byte[] pdfData, int pageIndex, int targetWidth) {
        try (PDDocument document = Loader.loadPDF(pdfData)) {
            if (pageIndex < 0 || pageIndex >= document.getNumberOfPages()) {
                throw new IndexOutOfBoundsException("Page index is out of bounds");
            }
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            BufferedImage image = pdfRenderer.renderImageWithDPI(pageIndex, 150);

            if (targetWidth <= 0) targetWidth = image.getWidth();

            int targetHeight = (int) (((double) image.getHeight() / image.getWidth()) * targetWidth);
            Image scaledImage = image.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
            return new ImageIcon(scaledImage);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int getPageCount(byte[] pdfData) {
        try (PDDocument document = Loader.loadPDF(pdfData)) {
            return document.getNumberOfPages();
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }
}