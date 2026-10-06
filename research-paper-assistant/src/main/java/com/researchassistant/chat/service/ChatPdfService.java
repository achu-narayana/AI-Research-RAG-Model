package com.researchassistant.chat.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import com.researchassistant.chat.entity.ChatMessage;
import com.researchassistant.chat.repository.ChatMessageRepository;
import com.researchassistant.chat.repository.ProjectChatRepository;
import com.researchassistant.project.entity.Project;
import com.researchassistant.project.repository.ProjectRepository;

@Service
public class ChatPdfService {

    private final ProjectRepository projectRepository;
    private final ProjectChatRepository projectChatRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatPdfService(
            ProjectRepository projectRepository,
            ProjectChatRepository projectChatRepository,
            ChatMessageRepository chatMessageRepository) {

        this.projectRepository = projectRepository;
        this.projectChatRepository = projectChatRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    public byte[] generateChatPdf(Long projectId, String email) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new RuntimeException("Project not found"));

        // Check project ownership
        if (!project.getOwner().getEmail().equals(email)) {
            throw new RuntimeException(
                    "You are not authorized to access this project");
        }

        var chat = projectChatRepository.findByProject(project)
                .orElseThrow(() ->
                        new RuntimeException(
                                "No chat exists for this project"));

        List<ChatMessage> messages =
                chatMessageRepository
                        .findAllByChatOrderByCreatedAtAsc(chat);

        try (
                PDDocument document = new PDDocument();
                ByteArrayOutputStream outputStream =
                        new ByteArrayOutputStream()
        ) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float margin = 50;
            float y = 780;
            float pageWidth = PDRectangle.A4.getWidth();

            PDPageContentStream contentStream =
                    new PDPageContentStream(document, page);

            // =====================================================
            // TITLE
            // =====================================================

            contentStream.beginText();

            contentStream.setFont(
                    new PDType1Font(
                            Standard14Fonts.FontName.HELVETICA_BOLD
                    ),
                    20
            );

            contentStream.newLineAtOffset(margin, y);

            contentStream.showText(
                    cleanText(
                            "AI Research Paper Assistant",
                            true
                    )
            );

            contentStream.endText();

            y -= 35;

            // =====================================================
            // PROJECT TITLE
            // =====================================================

            contentStream.beginText();

            contentStream.setFont(
                    new PDType1Font(
                            Standard14Fonts.FontName.HELVETICA_BOLD
                    ),
                    14
            );

            contentStream.newLineAtOffset(margin, y);

            contentStream.showText(
                    "Project: " +
                    cleanText(
                            project.getTitle(),
                            true
                    )
            );

            contentStream.endText();

            y -= 25;

            // =====================================================
            // CHAT HISTORY
            // =====================================================

            contentStream.beginText();

            contentStream.setFont(
                    new PDType1Font(
                            Standard14Fonts.FontName.HELVETICA
                    ),
                    10
            );

            contentStream.newLineAtOffset(margin, y);

            contentStream.showText(
                    cleanText(
                            "Chat History",
                            false
                    )
            );

            contentStream.endText();

            y -= 30;

            int questionNumber = 0;

            // =====================================================
            // CHAT MESSAGES
            // =====================================================

            for (ChatMessage message : messages) {

                String role = message.getRole();

                if ("USER".equalsIgnoreCase(role)) {

                    questionNumber++;

                    // -------------------------------------------------
                    // QUESTION HEADING
                    // -------------------------------------------------

                    y = writeWrappedText(
                            contentStream,
                            cleanText(
                                    "Question " + questionNumber,
                                    true
                            ),
                            margin,
                            y,
                            pageWidth - (2 * margin),
                            12,
                            true
                    );

                    y -= 5;

                    // -------------------------------------------------
                    // QUESTION TEXT
                    // -------------------------------------------------

                    String questionText =
                            cleanText(
                                    message.getContent(),
                                    false
                            );

                    y = writeWrappedText(
                            contentStream,
                            questionText,
                            margin,
                            y,
                            pageWidth - (2 * margin),
                            11,
                            false
                    );

                    y -= 15;

                } else if ("ASSISTANT".equalsIgnoreCase(role)) {

                    // -------------------------------------------------
                    // ANSWER HEADING
                    // -------------------------------------------------

                    y = writeWrappedText(
                            contentStream,
                            cleanText(
                                    "Answer " + questionNumber,
                                    true
                            ),
                            margin,
                            y,
                            pageWidth - (2 * margin),
                            12,
                            true
                    );

                    y -= 5;

                    // -------------------------------------------------
                    // ANSWER TEXT
                    // -------------------------------------------------

                    String answerText =
                            cleanText(
                                    message.getContent(),
                                    false
                            );

                    y = writeWrappedText(
                            contentStream,
                            answerText,
                            margin,
                            y,
                            pageWidth - (2 * margin),
                            11,
                            false
                    );

                    y -= 25;
                }

                // =====================================================
                // CREATE NEW PAGE WHEN NEEDED
                // =====================================================

                if (y < 70) {

                    contentStream.close();

                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);

                    contentStream =
                            new PDPageContentStream(
                                    document,
                                    page
                            );

                    y = 780;
                }
            }

            contentStream.close();

            document.save(outputStream);

            return outputStream.toByteArray();

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to generate chat PDF",
                    e
            );
        }
    }

    // =============================================================
    // WRITE WRAPPED TEXT
    // =============================================================

    private float writeWrappedText(
            PDPageContentStream contentStream,
            String text,
            float x,
            float y,
            float maxWidth,
            float fontSize,
            boolean bold
    ) throws IOException {

        PDType1Font font = getFont(bold);

        contentStream.setFont(
                font,
                fontSize
        );

        String[] paragraphs =
                text.split("\\n");

        for (String paragraph : paragraphs) {

            String[] words =
                    paragraph.split(" ");

            StringBuilder line =
                    new StringBuilder();

            for (String word : words) {

                String testLine =
                        line.length() == 0
                                ? word
                                : line + " " + word;

                float textWidth =
                        font.getStringWidth(testLine)
                                / 1000
                                * fontSize;

                if (textWidth > maxWidth) {

                    // Prevent trying to print an empty line
                    if (line.length() > 0) {

                        contentStream.beginText();

                        contentStream.newLineAtOffset(
                                x,
                                y
                        );

                        contentStream.showText(
                                line.toString()
                        );

                        contentStream.endText();

                        y -= fontSize + 4;
                    }

                    line =
                            new StringBuilder(word);

                } else {

                    line =
                            new StringBuilder(testLine);
                }
            }

            if (line.length() > 0) {

                contentStream.beginText();

                contentStream.newLineAtOffset(
                        x,
                        y
                );

                contentStream.showText(
                        line.toString()
                );

                contentStream.endText();

                y -= fontSize + 4;
            }

            y -= 3;
        }

        return y;
    }

    // =============================================================
    // GET PDF FONT
    // =============================================================

    private PDType1Font getFont(boolean bold) {

        return new PDType1Font(
                bold
                        ? Standard14Fonts.FontName.HELVETICA_BOLD
                        : Standard14Fonts.FontName.HELVETICA
        );
    }

    // =============================================================
    // CLEAN UNSUPPORTED CHARACTERS
    // =============================================================

    private String cleanText(
            String text,
            boolean bold
    ) {

        if (text == null) {
            return "";
        }

        PDType1Font font =
                getFont(bold);

        StringBuilder cleaned =
                new StringBuilder();

        /*
         * Check every Unicode code point individually.
         *
         * If Helvetica supports the character:
         *      keep it.
         *
         * If Helvetica does not support it:
         *      replace it with a space.
         *
         * This means we do NOT need to maintain
         * a list of problematic characters such as
         * τ, λ, √, —, ₹, etc.
         */

        for (int i = 0; i < text.length();) {

            int codePoint =
                    text.codePointAt(i);

            String character =
                    new String(
                            Character.toChars(codePoint)
                    );

            try {

                // Ask PDFBox whether this font
                // can encode the character.
                font.encode(character);

                // Supported character
                cleaned.append(character);

            } catch (IllegalArgumentException | IOException e) {

                // Unsupported character.
                // Use a space instead of deleting it
                // so words do not accidentally join.
                cleaned.append(' ');
            }

            i +=
                    Character.charCount(
                            codePoint
                    );
        }

        return cleaned.toString();
    }
}