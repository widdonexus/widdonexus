package com.widdo.nexus.cli.shell;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TableRenderer
 *
 * @author XYL
 * @date 2025/08/27 14:27
 * @since 0.0.1-SNAPSHOT
 */
@Component
public class TableRenderer {
    private boolean colorEnabled;
    private TableStyle style;

    public TableRenderer() {
        this.colorEnabled = true;
        this.style = new TableStyle();
        // 安装ANSI控制台支持
        AnsiConsole.systemInstall();
    }

    /**
     * 设置是否启用颜色输出
     */
    public void setColorEnabled(boolean enabled) {
        this.colorEnabled = enabled;
    }

    /**
     * 渲染二维列表数据为表格
     */
    public String renderTable(List<String> headers, List<List<String>> rows) {
        return renderTable(headers, rows, new HashMap<>());
    }

    /**
     * 渲染二维列表数据为表格，支持自定义列对齐方式
     */
    public String renderTable(List<String> headers, List<List<String>> rows,
                              Map<Integer, Alignment> columnAlignments) {
        if (headers == null || headers.isEmpty()) {
            return "No data to display";
        }

        // 确保所有行都有相同的列数
        int columnCount = headers.size();
        for (List<String> row : rows) {
            if (row.size() != columnCount) {
                // 填充缺失的列
                while (row.size() < columnCount) {
                    row.add("");
                }
            }
        }

        // 计算每列的最大宽度
        int[] columnWidths = new int[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnWidths[i] = headers.get(i).length();
            for (List<String> row : rows) {
                if (i < row.size()) {
                    String cell = row.get(i);
                    // 去除ANSI颜色代码后计算长度
                    int length = stripAnsiCodes(cell).length();
                    if (length > columnWidths[i]) {
                        columnWidths[i] = length;
                    }
                }
            }
            // 添加一些内边距
            columnWidths[i] += 2;
        }

        StringBuilder table = new StringBuilder();

        // 绘制上边框
        if (style.drawBorder) {
            table.append(drawHorizontalBorder(columnWidths, true)).append("\n");
        }

        // 绘制表头
        table.append(drawRow(headers, columnWidths, columnAlignments, true)).append("\n");

        // 绘制表头分隔线
        if (style.drawHeaderSeparator) {
            table.append(drawHorizontalSeparator(columnWidths, true)).append("\n");
        }

        // 绘制数据行
        for (int i = 0; i < rows.size(); i++) {
            table.append(drawRow(rows.get(i), columnWidths, columnAlignments, false)).append("\n");

            // 在最后一行后绘制下边框
            if (style.drawBorder && i == rows.size() - 1) {
                table.append(drawHorizontalBorder(columnWidths, false)).append("\n");
            }
        }

        return table.toString();
    }

    /**
     * 渲染Map列表为表格
     */
    public String renderTableFromMaps(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return "No data to display";
        }

        // 提取表头（所有Map的键的并集）
        List<String> headers = new ArrayList<>();
        for (Map<String, Object> row : data) {
            for (String key : row.keySet()) {
                if (!headers.contains(key)) {
                    headers.add(key);
                }
            }
        }

        // 转换为二维列表
        List<List<String>> rows = new ArrayList<>();
        for (Map<String, Object> rowMap : data) {
            List<String> row = new ArrayList<>();
            for (String header : headers) {
                Object value = rowMap.get(header);
                row.add(value != null ? value.toString() : "");
            }
            rows.add(row);
        }

        return renderTable(headers, rows);
    }

    /**
     * 绘制水平边框
     */
    private String drawHorizontalBorder(int[] columnWidths, boolean isTop) {
        if (!style.drawBorder) {
            return "";
        }

        StringBuilder line = new StringBuilder();
        for (int width : columnWidths) {
            line.append(style.cornerChar);
            line.append(String.valueOf(style.horizontalChar).repeat(width));
        }
        line.append(style.cornerChar);

        return applyColor(line.toString(), Color.BLUE);
    }

    /**
     * 绘制水平分隔线
     */
    private String drawHorizontalSeparator(int[] columnWidths, boolean isHeaderSeparator) {
        char lineChar = isHeaderSeparator ? style.headerSeparatorChar : style.horizontalChar;

        StringBuilder line = new StringBuilder();
        for (int width : columnWidths) {
            line.append(style.cornerChar);
            line.append(String.valueOf(lineChar).repeat(width));
        }
        line.append(style.cornerChar);

        return applyColor(line.toString(), Color.BLUE);
    }

    /**
     * 绘制一行数据
     */
    private String drawRow(List<String> cells, int[] columnWidths,
                           Map<Integer, Alignment> alignments, boolean isHeader) {
        StringBuilder row = new StringBuilder();

        if (style.drawBorder) {
            row.append(applyColor(String.valueOf(style.verticalChar), Color.BLUE));
        }

        for (int i = 0; i < cells.size(); i++) {
            String cell = cells.get(i);
            int width = columnWidths[i];
            Alignment alignment = alignments.getOrDefault(i, Alignment.LEFT);

            // 对齐文本
            String alignedCell = alignText(stripAnsiCodes(cell), width - 2, alignment);

            // 应用颜色
            if (isHeader) {
                // 加粗
                alignedCell = applyColor(alignedCell, Color.CYAN, true);
            } else {
                // 保留原始颜色或应用默认颜色
                if (containsAnsiCodes(cell)) {
                    // 如果已有颜色代码，保留它们
                    alignedCell = cell;
                }
            }

            row.append(" ").append(alignedCell).append(" ");
            if (style.drawBorder) {
                row.append(applyColor(String.valueOf(style.verticalChar), Color.BLUE));
            }
        }

        return row.toString();
    }

    /**
     * 文本对齐
     */
    private String alignText(String text, int width, Alignment alignment) {
        String stripped = stripAnsiCodes(text);
        int length = stripped.length();

        if (length >= width) {
            // 文本太长，不进行截断
            return text;
        }

        int padding = width - length;
        switch (alignment) {
            case LEFT:
                return text + " ".repeat(padding);
            case RIGHT:
                return " ".repeat(padding) + text;
            case CENTER:
                int leftPadding = padding / 2;
                int rightPadding = padding - leftPadding;
                return " ".repeat(leftPadding) + text + " ".repeat(rightPadding);
            default:
                return text;
        }
    }

    /**
     * 应用颜色
     */
    private String applyColor(String text, Color color) {
        return applyColor(text, color, false);
    }

    /**
     * 应用颜色和样式
     */
    private String applyColor(String text, Color color, boolean bold) {
        if (!colorEnabled) {
            return text;
        }

        Ansi ansi = Ansi.ansi();

        // 设置颜色
        switch (color) {
            case BLACK:
                ansi.fg(Ansi.Color.BLACK);
                break;
            case RED:
                ansi.fg(Ansi.Color.RED);
                break;
            case GREEN:
                ansi.fg(Ansi.Color.GREEN);
                break;
            case YELLOW:
                ansi.fg(Ansi.Color.YELLOW);
                break;
            case BLUE:
                ansi.fg(Ansi.Color.BLUE);
                break;
            case MAGENTA:
                ansi.fg(Ansi.Color.MAGENTA);
                break;
            case CYAN:
                ansi.fg(Ansi.Color.CYAN);
                break;
            case WHITE:
                ansi.fg(Ansi.Color.WHITE);
                break;
            default:
                ansi.fgDefault();
        }

        // 设置粗体
        if (bold) {
            ansi.bold();
        }

        ansi.a(text).reset();

        return ansi.toString();
    }

    /**
     * 去除ANSI颜色代码
     */
    private String stripAnsiCodes(String text) {
        return text.replaceAll("\u001B\\[[;\\d]*m", "");
    }

    /**
     * 检查字符串是否包含ANSI颜色代码
     */
    private boolean containsAnsiCodes(String text) {
        return text.matches(".*\u001B\\[[;\\d]*m.*");
    }

    /**
     * 设置表格样式
     */
    public void setStyle(char horizontalChar, char verticalChar, char cornerChar,
                         char headerSeparatorChar, boolean drawBorder, boolean drawHeaderSeparator) {
        this.style.horizontalChar = horizontalChar;
        this.style.verticalChar = verticalChar;
        this.style.cornerChar = cornerChar;
        this.style.headerSeparatorChar = headerSeparatorChar;
        this.style.drawBorder = drawBorder;
        this.style.drawHeaderSeparator = drawHeaderSeparator;
    }

    /**
     * 使用简约样式（无边框）
     */
    public void useMinimalStyle() {
        setStyle(' ', ' ', ' ', ' ', false, false);
    }

    /**
     * 使用完整样式（带边框）
     */
    public void useFullStyle() {
        setStyle('─', '│', '+', '=', true, true);
    }

    // 颜色定义
    public enum Color {
        BLACK, RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN, WHITE, DEFAULT
    }

    // 对齐方式
    public enum Alignment {
        LEFT, CENTER, RIGHT
    }

    // 表格样式配置
    private static class TableStyle {
        public char horizontalChar = '─';
        public char verticalChar = '│';
        public char cornerChar = '+';
        public char headerSeparatorChar = '=';
        public boolean drawBorder = true;
        public boolean drawHeaderSeparator = true;
    }
}
