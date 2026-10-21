package com.cab.service;

import com.cab.model.*;
import com.cab.repo.*;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;

@Service @RequiredArgsConstructor @Slf4j
public class ReceiptService {
    private final RideService rides; private final RideRepository rideRepo; private final PaymentRepository payments;
    private final ObjectProvider<JavaMailSender> mail;
    @Value("${app.mail-from}") String from;

    /** Invoice PDF (iText 7). */
    public byte[] pdf(String email, Long rideId) { return build(rides.accessible(email, rideId)); }

    private byte[] build(Ride r) {
        Payment p = payments.findByRideId(r.getId()).orElse(null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Document doc = new Document(new PdfDocument(new PdfWriter(out)))) {
            doc.add(new Paragraph("CabGo - Ride Receipt").setBold().setFontSize(20));
            doc.add(new Paragraph("Receipt no: " + (p != null && p.getReceiptNo() != null ? p.getReceiptNo() : "N/A (unpaid)")));
            Table t = new Table(2);
            row(t, "Ride #", String.valueOf(r.getId())); row(t, "Date", String.valueOf(r.getCreatedAt()));
            row(t, "Rider", r.getRider().getName());
            row(t, "Driver", r.getDriver() == null ? "-" : r.getDriver().getUser().getName() + " (" + r.getDriver().getPlate() + ")");
            row(t, "Pickup", String.valueOf(r.getPickupAddress())); row(t, "Drop", String.valueOf(r.getDropAddress()));
            row(t, "Distance", r.getDistanceKm() + " km"); row(t, "Discount", "INR " + r.getDiscount());
            row(t, "Total", "INR " + r.getFare());
            row(t, "Payment", p == null ? "UNPAID" : p.getStatus().name());
            doc.add(t);
        }
        return out.toByteArray();
    }
    private static void row(Table t, String k, String v) { t.addCell(new Cell().add(new Paragraph(k).setBold())); t.addCell(new Cell().add(new Paragraph(v))); }

    /** E-mail receipt with PDF attached (JavaMailSender). Silently skipped when SMTP is not configured. */
    @Async
    public void email(Long rideId) {
        JavaMailSender sender = mail.getIfAvailable();
        if (sender == null) { log.info("Mail not configured (profile 'mail'); skipping receipt e-mail for ride {}", rideId); return; }
        try {
            Ride r = rideRepo.findById(rideId).orElseThrow();
            MimeMessage m = sender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(m, true);
            h.setFrom(from); h.setTo(r.getRider().getEmail()); h.setSubject("Your CabGo receipt for ride #" + rideId);
            h.setText("Thanks for riding with CabGo. Your receipt is attached.");
            h.addAttachment("receipt-" + rideId + ".pdf", new ByteArrayResource(build(r)));
            sender.send(m);
        } catch (Exception e) { log.warn("Could not send receipt e-mail: {}", e.getMessage()); }
    }
}
