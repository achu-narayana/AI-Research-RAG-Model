package com.researchassistant.chat.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
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
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Check project ownership
        if (!project.getOwner().getEmail().equals(email)) {
            throw new RuntimeException(
                    "You are not authorized to access this project");
        }

        var chat = projectChatRepository.findByProject(project)
                .orElseThrow(() -> new RuntimeException(
                        "No chat exists for this project"));

        List<ChatMessage> messages =
                chatMessageRepository.findAllByChatOrderByCreatedAtAsc(chat);

        try (
            PDDocument document = new PDDocument();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float margin = 50;
            float y = 780;
            float pageWidth = PDRectangle.A4.getWidth();

            PDPageContentStream contentStream =
                    new PDPageContentStream(document, page);

            // Title
            contentStream.beginText();
            contentStream.setFont(
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),
                    20
            );
            contentStream.newLineAtOffset(margin, y);
            contentStream.showText("AI Research Paper Assistant");
            contentStream.endText();

            y -= 35;

            // Project title
            contentStream.beginText();
            contentStream.setFont(
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),
                    14
            );
            contentStream.newLineAtOffset(margin, y);
            contentStream.showText(
                    "Project: " + cleanText(project.getTitle())
            );
            contentStream.endText();

            y -= 25;

            // Generated date
            contentStream.beginText();
            contentStream.setFont(
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                    10
            );
            contentStream.newLineAtOffset(margin, y);
            contentStream.showText(
                    "Chat History"
            );
            contentStream.endText();

            y -= 30;

            int questionNumber = 0;

            for (ChatMessage message : messages) {

                String role = message.getRole();
                String text = cleanText(message.getContent());

                if ("USER".equalsIgnoreCase(role)) {

                    questionNumber++;

                    // Question heading
                    y = writeWrappedText(
                            contentStream,
                            "Question " + questionNumber,
                            margin,
                            y,
                            pageWidth - (2 * margin),
                            12,
                            true
                    );

                    y -= 5;

                    y = writeWrappedText(
                            contentStream,
                            text,
                            margin,
                            y,
                            pageWidth - (2 * margin),
                            11,
                            false
                    );

                    y -= 15;

                } else if ("ASSISTANT".equalsIgnoreCase(role)) {

                    y = writeWrappedText(
                            contentStream,
                            "Answer " + questionNumber,
                            margin,
                            y,
                            pageWidth - (2 * margin),
                            12,
                            true
                    );

                    y -= 5;

                    y = writeWrappedText(
                            contentStream,
                            text,
                            margin,
                            y,
                            pageWidth - (2 * margin),
                            11,
                            false
                    );

                    y -= 25;
                }

                // Create a new page if required
                if (y < 70) {

                    contentStream.close();

                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);

                    contentStream =
                            new PDPageContentStream(document, page);

                    y = 780;
                }
            }

            contentStream.close();

            document.save(outputStream);

            return outputStream.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to generate chat PDF", e);
        }
    }

    private float writeWrappedText(
            PDPageContentStream contentStream,
            String text,
            float x,
            float y,
            float maxWidth,
            float fontSize,
            boolean bold) throws IOException {

        PDType1Font font = new PDType1Font(
                bold
                        ? Standard14Fonts.FontName.HELVETICA_BOLD
                        : Standard14Fonts.FontName.HELVETICA
        );

        contentStream.setFont(font, fontSize);

        String[] paragraphs = text.split("\\n");

        for (String paragraph : paragraphs) {

            String[] words = paragraph.split(" ");
            StringBuilder line = new StringBuilder();

            for (String word : words) {

                String testLine = line.length() == 0
                        ? word
                        : line + " " + word;

                float textWidth =
                        font.getStringWidth(testLine)
                        / 1000 * fontSize;

                if (textWidth > maxWidth) {

                    contentStream.beginText();
                    contentStream.newLineAtOffset(x, y);
                    contentStream.showText(line.toString());
                    contentStream.endText();

                    y -= fontSize + 4;

                    line = new StringBuilder(word);

                } else {

                    line = new StringBuilder(testLine);
                }
            }

            if (line.length() > 0) {

                contentStream.beginText();
                contentStream.newLineAtOffset(x, y);
                contentStream.showText(line.toString());
                contentStream.endText();

                y -= fontSize + 4;
            }

            y -= 3;
        }

        return y;
    }

    private String cleanText(String text) {

        if (text == null) {
            return "";
        }

        return text
                // Currency
                .replace('\u20B9', 'R')

                // Spaces
                .replace('\u00A0', ' ')   // non-breaking space
                .replace('\u202F', ' ')   // narrow no-break space
                .replace('\u2007', ' ')   // figure space
                .replace('\u2009', ' ')   // thin space
                .replace('\u200A', ' ')   // hair space

                // Hyphens / dashes
                .replace('\u2010', '-')
                .replace('\u2011', '-')
                .replace('\u2012', '-')
                .replace('\u2013', '-')
                .replace('\u2014', '-')

                // Quotes
                .replace('\u2018', '\'')
                .replace('\u2019', '\'')
                .replace('\u201A', '\'')
                .replace('\u201B', '\'')
                .replace('\u201C', '"')
                .replace('\u201D', '"')
                .replace('\u201E', '"')
                .replace('\u201F', '"')

                // Bullet
                .replace('\u2022', '-');
    }
    
}