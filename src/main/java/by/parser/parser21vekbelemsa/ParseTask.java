package by.parser.parser21vekbelemsa;

import javafx.concurrent.Task;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ParseTask extends Task<Void> {
    private final String path;
    private final String fileRRCPath;
    private final ArrayList<String> brandList;

    public ParseTask(String path, String fileRRCPath, ArrayList<String> brandList) {
        this.path = path;
        this.fileRRCPath = fileRRCPath;
        this.brandList = brandList;
    }

    private final List<Nomenclature> nomenclatures = new ArrayList<>();

    @Override

    protected Void call() throws Exception {
        StringBuilder brandFilter = new StringBuilder();
        for (String brand : brandList) {
            brandFilter.append("&filter[producer][]=").append(brand);
        }

        List<String> categoryList = getCategoriesList();
        for (String category : categoryList) {
            collectNomenclatures(category, 1, String.valueOf(brandFilter));
        }

        createExcel();
        return null;
    }

    private void collectNomenclatures(String category, int page, String brandFilter) throws IOException, InterruptedException {
        int minDelayMillis = 2500;
        int maxDelayMillis = 8000;

        int randomDelay = minDelayMillis + new Random().nextInt(maxDelayMillis - minDelayMillis + 1);
        Thread.sleep(randomDelay);

        String baseUrl = "https://www.21vek.by/";
        String pagePart = "/page:";

        String resultUri = page == 1 ? baseUrl + category + "/all/?filter[good_status][]=in" + brandFilter : baseUrl + category + pagePart + page + "/?filter[good_status][]=in" + brandFilter;

        Document document = Jsoup.connect(resultUri)
                .userAgent(getUserAgent())
                .timeout(5000)
                .ignoreContentType(true)
                .ignoreHttpErrors(false)
                .referrer(Const.REFERRER)
                .get();

        Element productBlock = document.body().getElementById("j-search_result");
        if (productBlock != null) {
            Elements prodactElements = Objects.requireNonNull(productBlock).getElementsByAttributeValueStarting("class", "g-item-data j-item-data");
            prodactElements.forEach(element -> {
                String id = element.attr("data-code");
                String name = element.attr("data-name");
                String producer = element.attr("data-producer_name");
                String categoryName = element.attr("data-category");
                String oldPrice = element.attr("data-old_price");
                String price = element.attr("data-price");
                String link = productBlock.tagName("a").getElementsByAttributeValue("data-code", id).get(0).attr("href");

                nomenclatures.add(new Nomenclature(Long.parseLong(id), name, producer, categoryName, oldPrice, price, link));
            });
        }

        Element paginator = document.body().getElementById("j-paginator");
        if (paginator != null) {
            boolean isHasNext = !paginator.getElementsByAttributeValue("rel", "next").isEmpty();
            if (isHasNext){
                int nextPage = Integer.parseInt(Objects.requireNonNull(paginator.getElementsByAttributeValue("rel", "next").first()).attr("name"));
                collectNomenclatures(category, nextPage, brandFilter);
            }
        }
    }

    private List<String> getCategoriesList() throws IOException {
        List<String> list = new ArrayList<>();

        BufferedReader reader = new BufferedReader(new FileReader(Const.CONFIG_FILE_NAME));
        String line = reader.readLine();

        while (line != null) {
            if (line.contains("=")) {
                String trimmed = line.substring(line.indexOf("=") + 1).trim();
                if (line.startsWith(Const.CATEGORIES)) {
                    list = Stream.of(trimmed.split(","))
                            .map(String::trim)
                            .collect(Collectors.toList());
                    break;
                }
            }
            line = reader.readLine();
        }
        reader.close();

        return list;
    }

    private String getUserAgent() throws IOException {
        String result = null;
        BufferedReader reader = new BufferedReader(new FileReader(Const.CONFIG_FILE_NAME));
        String line = reader.readLine();

        while (line != null) {
            if (line.contains("=")) {
                String trimmed = line.substring(line.indexOf("=") + 1).trim();
                if (line.startsWith(Const.USER_AGENT)) {
                    result = trimmed;
                    break;
                }
            }
            line = reader.readLine();
        }
        reader.close();

        return result;
    }

    private void createExcel(){
        updateMessage("Создание excel файла...");
        updateProgress(9100, 10000);
        int startRow = 1;
        int startColumn = 1;

        //Создаю книгу excel
        try(XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFDataFormat df = wb.createDataFormat();
            CreationHelper creationHelper = wb.getCreationHelper();
            XSSFSheet sheet = wb.createSheet("Результаты");

            //стиль ячеек шапки
            CellStyle headStyle = wb.createCellStyle();
            headStyle.setAlignment(HorizontalAlignment.CENTER);
            headStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headStyle.setBorderBottom(BorderStyle.MEDIUM);
            headStyle.setBorderLeft(BorderStyle.MEDIUM);
            headStyle.setBorderTop(BorderStyle.MEDIUM);
            headStyle.setBorderRight(BorderStyle.MEDIUM);

            // стиль для основной табличной части
            CellStyle contentDecimalStyle = wb.createCellStyle();
            contentDecimalStyle.setVerticalAlignment(VerticalAlignment.TOP);
            contentDecimalStyle.setBorderBottom(BorderStyle.THIN);
            contentDecimalStyle.setBorderLeft(BorderStyle.THIN);
            contentDecimalStyle.setBorderTop(BorderStyle.THIN);
            contentDecimalStyle.setBorderRight(BorderStyle.THIN);
            contentDecimalStyle.setDataFormat(df.getFormat("0.00"));

            // стиль для основной табличной части
            CellStyle contentStyle = wb.createCellStyle();
            contentStyle.setVerticalAlignment(VerticalAlignment.TOP);
            contentStyle.setBorderBottom(BorderStyle.THIN);
            contentStyle.setBorderLeft(BorderStyle.THIN);
            contentStyle.setBorderTop(BorderStyle.THIN);
            contentStyle.setBorderRight(BorderStyle.THIN);
            contentStyle.setDataFormat(df.getFormat("#"));

            //шрифты шапки
            Font headFont = wb.createFont();
            headFont.setFontName("Times New Roman");
            headFont.setBold(true);
            headFont.setFontHeightInPoints((short) 12);
            headStyle.setFont(headFont);

            XSSFRow head = sheet.createRow(startRow);

            XSSFCell idHead = head.createCell(startColumn);
            idHead.setCellValue("Код товара");
            headStyle.setWrapText(true);
            idHead.setCellStyle(headStyle);

            XSSFCell nameHead = head.createCell(++startColumn);
            nameHead.setCellValue("Наименование");
            headStyle.setWrapText(true);
            nameHead.setCellStyle(headStyle);

            XSSFCell brandHead = head.createCell(++startColumn);
            brandHead.setCellValue("Бренд");
            headStyle.setWrapText(true);
            brandHead.setCellStyle(headStyle);

            XSSFCell categoryHead = head.createCell(++startColumn);
            categoryHead.setCellValue("Категория на сайте");
            headStyle.setWrapText(true);
            categoryHead.setCellStyle(headStyle);

            XSSFCell priceHead = head.createCell(++startColumn);
            priceHead.setCellValue("Цена, руб.");
            headStyle.setWrapText(true);
            priceHead.setCellStyle(headStyle);

            XSSFCell oldPriceHead = head.createCell(++startColumn);
            oldPriceHead.setCellValue("Цена до скидки, руб.");
            headStyle.setWrapText(true);
            oldPriceHead.setCellStyle(headStyle);

            XSSFCell rrcHead = head.createCell(++startColumn);
            rrcHead.setCellValue("РРЦ, руб.");
            headStyle.setWrapText(true);
            rrcHead.setCellStyle(headStyle);

            XSSFCell deviationHead = head.createCell(++startColumn);
            deviationHead.setCellValue("Отклонение от РРЦ, %.");
            headStyle.setWrapText(true);
            deviationHead.setCellStyle(headStyle);

            sheet.setAutoFilter(CellRangeAddress.valueOf("B2:I2"));
            sheet.createFreezePane(0, 2);

            updateProgress(9300, 10000);

            int startRowContent = startRow + 1;

            for (Nomenclature nom : nomenclatures) {
                updateMessage("Внесение данных по " + nom.name() + " в файл excel");
                int startColumnContent = 1;
                XSSFRow content = sheet.createRow(startRowContent);

                //Код товара
                XSSFCell id = content.createCell(startColumnContent);
                contentStyle.setWrapText(true);
                id.setCellValue(nom.id());
                id.setCellStyle(contentStyle);
                sheet.setColumnWidth(startColumnContent, 4833);

                //Наименование товара в excel
                XSSFCell cellName = content.createCell(++startColumnContent);
                contentStyle.setWrapText(true);
                cellName.setCellValue(nom.name());
                XSSFHyperlink hl = (XSSFHyperlink) creationHelper.createHyperlink(HyperlinkType.URL);
                hl.setAddress(nom.link());
                cellName.setHyperlink(hl);
                cellName.setCellStyle(contentStyle);
                sheet.setColumnWidth(startColumnContent, 9300);

                //Бренд
                XSSFCell brand = content.createCell(++startColumnContent);
                contentStyle.setWrapText(true);
                brand.setCellValue(nom.producer());
                brand.setCellStyle(contentStyle);
                sheet.setColumnWidth(startColumnContent, 3808);

                //Категория
                XSSFCell category = content.createCell(++startColumnContent);
                contentStyle.setWrapText(true);
                category.setCellValue(nom.category());
                category.setCellStyle(contentStyle);
                sheet.setColumnWidth(startColumnContent, 5292);

                //Цена
                XSSFCell price = content.createCell(++startColumnContent);
                contentStyle.setWrapText(true);
                price.setCellValue(Double.parseDouble(nom.price()));
                price.setCellStyle(contentDecimalStyle);
                sheet.setColumnWidth(startColumnContent, 3808);

                //Старая цена
                XSSFCell oldPrice = content.createCell(++startColumnContent);
                contentStyle.setWrapText(true);
                if (!Objects.equals(nom.oldPrice(), "")){
                    oldPrice.setCellValue(Double.parseDouble(nom.oldPrice()));
                }
                oldPrice.setCellStyle(contentDecimalStyle);
                sheet.setColumnWidth(startColumnContent, 3808);

                //РРЦ
                XSSFCell rrc = content.createCell(++startColumnContent);
                contentStyle.setWrapText(true);
                rrc.setCellValue(getRRCFromExcel(nom.id()));
                rrc.setCellStyle(contentDecimalStyle);
                sheet.setColumnWidth(startColumnContent, 3808);

                //Отклонение
                XSSFCell deviation = content.createCell(++startColumnContent);
                contentStyle.setWrapText(true);
                deviation.setCellValue(Double.parseDouble(nom.price()));
                deviation.setCellStyle(contentDecimalStyle);
                sheet.setColumnWidth(startColumnContent, 3808);

                startRowContent++;
            }

            try (FileOutputStream fos = new FileOutputStream(path)) {
                wb.write(fos);
                fos.close();
                updateMessage("Сохранение файла по пути " + path);
                updateProgress(10000, 10000);

            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private double getRRCFromExcel(long id) throws IOException {
        double rrc = 0d;
        int rowStart = 4;

        XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(fileRRCPath));
        XSSFSheet sheet = workbook.getSheet("Лист1");
        for (int i = 0; i < sheet.getLastRowNum() - 1; i++) {
            XSSFRow row = sheet.getRow(rowStart);

            if (row.getCell(16) != null) {
                if (row.getCell(16).getNumericCellValue() == id) {
                    rrc = row.getCell(13).getNumericCellValue();
                    break;
                }
                rowStart++;
            }
        }

        workbook.close();
        return rrc;
    }
}
