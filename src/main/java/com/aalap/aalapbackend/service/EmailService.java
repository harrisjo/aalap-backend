package com.aalap.aalapbackend.service;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around the Resend SDK.
 * Every public method is fire-and-forget: exceptions are caught and logged so
 * that a transient email failure never rolls back a business operation.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final Resend   resend;
    private final String   fromAddress;

    public EmailService(
            @Value("${resend.api-key}") String apiKey,
            @Value("${resend.from-address}") String fromAddress) {
        this.resend      = new Resend(apiKey);
        this.fromAddress = fromAddress;
    }

    // ─── PUBLIC API ──────────────────────────────────────────────────────────────

    public void sendWelcomeEmail(String toEmail, String name) {
        send(toEmail,
             "Welcome to Aalap 🎵",
             buildWelcomeHtml(name));
    }

    public void sendContributionNotification(
            String toEmail,
            String threadOwnerName,
            String threadTitle,
            String contributorName,
            String role,
            Long   threadId) {

        send(toEmail,
             contributorName + " just added a stem to your thread",
             buildContributionHtml(threadOwnerName, threadTitle, contributorName, role, threadId));
    }

    // ─── PRIVATE HELPERS ─────────────────────────────────────────────────────────

    private void send(String to, String subject, String html) {
        if (to == null || to.isBlank() || to.contains("@aalap.invalid")) {
            return; // never send to soft-deleted placeholder addresses
        }
        try {
            CreateEmailOptions options = CreateEmailOptions.builder()
                    .from(fromAddress)
                    .to(to)
                    .subject(subject)
                    .html(html)
                    .build();
            resend.emails().send(options);
        } catch (Exception e) {
            log.warn("Resend: failed to deliver '{}' to {}: {}", subject, to, e.getMessage());
        }
    }

    private String buildWelcomeHtml(String name) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <body style="margin:0;padding:0;background:#060808;font-family:'DM Sans',sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" border="0">
                <tr><td align="center" style="padding:48px 16px;">
                  <table width="560" cellpadding="0" cellspacing="0" border="0"
                         style="background:#0d1614;border:1px solid rgba(255,212,202,0.1);
                                border-radius:16px;overflow:hidden;">
                    <tr>
                      <td style="padding:40px 40px 0;border-bottom:1px solid rgba(71,91,90,0.4);">
                        <p style="margin:0 0 6px;font-size:11px;letter-spacing:3px;text-transform:uppercase;
                                  color:rgba(255,212,202,0.4);">You're in</p>
                        <h1 style="margin:0 0 24px;font-size:30px;color:#FCFCFC;font-weight:700;
                                   line-height:1.2;">Welcome, %s 👋</h1>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:32px 40px;">
                        <p style="margin:0 0 16px;font-size:15px;line-height:1.7;
                                  color:rgba(255,255,255,0.55);">
                          You're now part of a growing community of musicians collaborating across
                          instruments, genres, and borders.
                        </p>
                        <p style="margin:0 0 32px;font-size:15px;line-height:1.7;
                                  color:rgba(255,255,255,0.55);">
                          Start a thread, drop a stem, and let the music build itself.
                        </p>
                        <a href="https://aalapmusic.vercel.app/home"
                           style="display:inline-block;padding:14px 28px;background:#FF4439;
                                  color:#fff;text-decoration:none;border-radius:10px;
                                  font-weight:700;font-size:14px;letter-spacing:0.3px;">
                          Open Aalap →
                        </a>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:20px 40px;background:#060808;">
                        <p style="margin:0;font-size:11px;color:rgba(255,255,255,0.2);">
                          You're receiving this because you just created an account on Aalap.
                        </p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(name);
    }

    private String buildContributionHtml(
            String ownerName, String threadTitle,
            String contributorName, String role, Long threadId) {

        String url = "https://aalapmusic.vercel.app/threads/" + threadId;
        return """
            <!DOCTYPE html>
            <html lang="en">
            <body style="margin:0;padding:0;background:#060808;font-family:'DM Sans',sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" border="0">
                <tr><td align="center" style="padding:48px 16px;">
                  <table width="560" cellpadding="0" cellspacing="0" border="0"
                         style="background:#0d1614;border:1px solid rgba(255,212,202,0.1);
                                border-radius:16px;overflow:hidden;">
                    <tr>
                      <td style="padding:40px 40px 0;">
                        <p style="margin:0 0 6px;font-size:11px;letter-spacing:3px;text-transform:uppercase;
                                  color:rgba(255,212,202,0.4);">New stem in your session</p>
                        <h1 style="margin:0 0 8px;font-size:26px;color:#FCFCFC;font-weight:700;
                                   line-height:1.2;">%s</h1>
                        <p style="margin:0 0 32px;font-size:15px;color:rgba(255,255,255,0.5);
                                  line-height:1.6;">
                          <span style="color:#FF4439;font-weight:600;">%s</span>
                          just added a
                          <span style="color:#FFD4CA;font-weight:600;">%s</span>
                          stem to your thread.
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:0 40px 40px;">
                        <a href="%s"
                           style="display:inline-block;padding:14px 28px;background:#FF4439;
                                  color:#fff;text-decoration:none;border-radius:10px;
                                  font-weight:700;font-size:14px;letter-spacing:0.3px;">
                          Listen now →
                        </a>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:20px 40px;background:#060808;border-top:1px solid rgba(71,91,90,0.4);">
                        <p style="margin:0;font-size:11px;color:rgba(255,255,255,0.2);">
                          You're receiving this because you created this thread on Aalap.
                        </p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(threadTitle, contributorName, role, url);
    }
}

