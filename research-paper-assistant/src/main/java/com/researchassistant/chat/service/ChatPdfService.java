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
import org.springframework.transaction.annotation.Transactional;

import com.researchassistant.chat.entity.ChatMessage;
import com.researchassistant.chat.entity.ProjectChat;
import com.researchassistant.chat.repository.ChatMessageRepository;
import com.researchassistant.chat.repository.ProjectChatRepository;
import com.researchassistant.common.exception.NotFoundException;
import com.researchassistant.project.entity.Project;
import com.researchassistant.project.service.ProjectAccessService;

@Service
public class ChatPdfService {

    private static final float MARGIN = 50;
    private static final float TOP_Y = 780;
    private static final float BOTTOM_MARGIN = 60;
    private static final float LINE_SPACING = 4;

    private static final PDType1Font REGULAR_FONT =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    private static final PDType1Font BOLD_FONT =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private final ProjectAccessService projectAccessService;
    private final ProjectChatRepository projectChatRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatPdfService(
            ProjectAccessService projectAccessService,
            ProjectChatRepository projectChatRepository,
            ChatMessageRepository chatMessageRepository) {

        this.projectAccessService = projectAccessService;
        this.projectChatRepository = projectChatRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional(readOnly = true)
    public byte[] generateChatPdf(Long projectId, String email) {

        // Project exists and belongs to the user
        Project project =
                projectAccessService.getOwnedProject(projectId, email);

        ProjectChat chat = projectChatRepository.findByProject(project)
                .orElseThrow(() ->
                        new NotFoundException(
                                "No chat exists for this project"));

        List<ChatMessage> messages =
                chatMessageRepository
                        .findAllByChatOrderByIdAsc(chat);

        try (
                PDDocument document = new PDDocument();
                ByteArrayOutputStream outputStream =
                        new ByteArrayOutputStream()
        ) {

            PdfWriter writer = new PdfWriter(document);

            try {

                // =================================================
                // TITLE
                // =================================================

                writeWrappedText(writer, "AI Research Paper Assistant", 20, true);
                writer.space(15);

                // =================================================
                // PROJECT TITLE
                // =================================================

                writeWrappedText(writer, "Project: " + project.getTitle(), 14, true);
                writer.space(6);

                writeWrappedText(writer, "Chat History", 10, false);
                writer.space(20);

                // =================================================
                // CHAT MESSAGES
                // =================================================

                int questionNumber = 0;

                for (ChatMessage message : messages) {

                    String type = message.getMessageType() == null
                            ? ChatMessage.TYPE_CHAT
                            : message.getMessageType();

                    boolean isUser =
                            "USER".equalsIgnoreCase(message.getRole());

                    String heading;

                    if (ChatMessage.TYPE_SUMMARY.equalsIgnoreCase(type)) {
                        heading = "Summary";

                    } else if (ChatMessage.TYPE_COMPARISON.equalsIgnoreCase(type)) {
                        heading = "Comparison";

                    } else if (isUser) {
                        questionNumber++;
                        heading = "Question " + questionNumber;

                    } else {
                        heading = "Answer " + questionNumber;
                    }

                    // Heading
                    writeWrappedText(writer, heading, 12, true);
                    writer.space(5);

                    // Body
                    writeWrappedText(writer, message.getContent(), 11, false);
                    writer.space(isUser ? 15 : 25);
                }

            } finally {
                writer.close();
            }

            document.save(outputStream);

            return outputStream.toByteArray();

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to generate chat PDF",
                    e
            );
        }
    }

    // =============================================================
    // PAGE-AWARE WRITER
    // =============================================================

    /**
     * Keeps track of the current page, content stream and y
     * position, and starts a new page when a line would fall
     * below the bottom margin.
     */
    private static final class PdfWriter {

        private final PDDocument document;
        private PDPageContentStream contentStream;
        private float y;

        PdfWriter(PDDocument document) throws IOException {
            this.document = document;
            newPage();
        }

        float maxWidth() {
            return PDRectangle.A4.getWidth() - (2 * MARGIN);
        }

        void newPage() throws IOException {

            if (contentStream != null) {
                contentStream.close();
            }

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            contentStream = new PDPageContentStream(document, page);
            y = TOP_Y;
        }

        void writeLine(
                String text,
                PDType1Font font,
                float fontSize) throws IOException {

            if (y < BOTTOM_MARGIN) {
                newPage();
            }

            if (!text.isEmpty()) {
                contentStream.beginText();
                contentStream.setFont(font, fontSize);
                contentStream.newLineAtOffset(MARGIN, y);
                contentStream.showText(text);
                contentStream.endText();
            }

            y -= fontSize + LINE_SPACING;
        }

        void space(float amount) {
            y -= amount;
        }

        void close() throws IOException {
            if (contentStream != null) {
                contentStream.close();
                contentStream = null;
            }
        }
    }

    // =============================================================
    // WRITE WRAPPED TEXT
    // =============================================================

    private void writeWrappedText(
            PdfWriter writer,
            String text,
            float fontSize,
            boolean bold
    ) throws IOException {

        if (text == null) {
            return;
        }

        PDType1Font font = getFont(bold);
        float maxWidth = writer.maxWidth();

        // Split on newlines BEFORE cleaning so paragraphs survive
        String[] rawLines = text.split("\\r?\\n", -1);

        for (String rawLine : rawLines) {

            String line =
                    cleanText(stripMarkdown(rawLine), bold).strip();

            if (line.isEmpty()) {
                // Blank line = paragraph gap
                writer.space(fontSize / 2);
                continue;
            }

            StringBuilder current = new StringBuilder();

            for (String word : line.split(" +")) {

                if (word.isEmpty()) {
                    continue;
                }

                // Hard-break words wider than the whole line
                if (textWidth(font, word, fontSize) > maxWidth) {

                    if (current.length() > 0) {
                        writer.writeLine(current.toString(), font, fontSize);
                        current.setLength(0);
                    }

                    StringBuilder chunk = new StringBuilder();

                    for (int i = 0; i < word.length(); ) {

                        int codePoint = word.codePointAt(i);
                        String character = new String(Character.toChars(codePoint));

                        if (chunk.length() > 0 &&
                                textWidth(font, chunk + character, fontSize) > maxWidth) {

                            writer.writeLine(chunk.toString(), font, fontSize);
                            chunk.setLength(0);
                        }

                        chunk.append(character);
                        i += Character.charCount(codePoint);
                    }

                    current.append(chunk);
                    continue;
                }

                String testLine =
                        current.length() == 0
                                ? word
                                : current + " " + word;

                if (textWidth(font, testLine, fontSize) > maxWidth) {

                    writer.writeLine(current.toString(), font, fontSize);
                    current = new StringBuilder(word);

                } else {

                    current = new StringBuilder(testLine);
                }
            }

            if (current.length() > 0) {
                writer.writeLine(current.toString(), font, fontSize);
            }

            writer.space(3);
        }
    }

    private float textWidth(
            PDType1Font font,
            String text,
            float fontSize) throws IOException {

        return font.getStringWidth(text) / 1000 * fontSize;
    }

    // =============================================================
    // STRIP SIMPLE MARKDOWN
    // =============================================================

    private String stripMarkdown(String line) {

        return line
                // Leading heading markers: "## Title" -> "Title"
                .replaceFirst("^\\s*#{1,6}\\s*", "")
                // Bullet "* item" -> "- item"
                .replaceFirst("^(\\s*)\\*\\s+", "$1- ")
                .replace("**", "")
                .replace("__", "")
                .replace("`", "");
    }

    // =============================================================
    // GET PDF FONT
    // =============================================================

    private PDType1Font getFont(boolean bold) {

        return bold ? BOLD_FONT : REGULAR_FONT;
    }

    // =============================================================
    // CLEAN UNSUPPORTED CHARACTERS
    // =============================================================

    /**
     * Replaces every code point Helvetica cannot encode (including
     * control characters such as tabs) with a space. Must be called
     * on single lines: newlines are not preserved.
     */
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
