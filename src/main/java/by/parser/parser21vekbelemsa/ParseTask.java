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
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;

import java.io.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static by.parser.parser21vekbelemsa.Const.BASE_URL;

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
    private final List<String> links = new ArrayList<>();
    private final List<String> linksToParse = new ArrayList<>();
    private int currentProgressValue = 0;

    @Override

    protected Void call() throws Exception {
        updateMessage("Подключение к серверу...");
        updateProgress(currentProgressValue, 10000);

        int brandCost = 1000 / brandList.size();

        for (String brand : brandList) {
            collectCategoryValues(brand);

            currentProgressValue += brandCost;

            updateProgress(currentProgressValue, 10000);
            updateMessage("Сбор ссылок по бренду " + brand);
        }

        updateMessage("Запуск браузера....");

        EdgeOptions options = new EdgeOptions();
        options.addArguments("--remote-allow-origins=*");
//        options.addArguments("--headless");

        WebDriver driver = new EdgeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
        driver.manage().window().maximize();

        System.setProperty("webdriver.chrome.driver", "chromedriver.exe");


        int seleniumLinkCost = 3000 / linksToParse.size();
        for (String link : linksToParse) {
            updateMessage("Переход по ссылке " + link);
            collectLinks(link, driver);

            currentProgressValue += seleniumLinkCost;

            updateProgress(currentProgressValue, 10000);
        }

        driver.quit();

        collectNomenclatures();

        updateProgress( 9000, 10000);
        createExcel();
        return null;
    }

    private void collectCategoryValues(String brand) throws IOException {
        String url = BASE_URL + "info/brands/" + brand + ".html";

        Document document = Jsoup.connect(url)
                .userAgent(getUserAgent())
                .timeout(5000)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .referrer(Const.REFERRER)
                .get();

        Elements categoryBlock = document.body().getElementsByAttributeValueStarting("class", "b-categories-full");

        if (categoryBlock.isEmpty()) {
            categoryBlock = document.body().getElementsByAttributeValueStarting("class", "b-categories-popular");
        }

        Elements tagLiList = categoryBlock.get(0).getElementsByAttribute("href");

        for (Element element : tagLiList) {
            linksToParse.add(element.attr("href"));
        }
    }

    private void collectLinks(String linkToParse, @org.jetbrains.annotations.NotNull WebDriver driver) {

        JavascriptExecutor jse = (JavascriptExecutor) driver;

        driver.get(linkToParse);
        jse.executeScript("window.scrollBy(0,2500)", "");

        List<WebElement> goods = driver.findElements(By.xpath("//div[@data-testid='product-list']//div[starts-with(@class, 'style_product')]//p[starts-with(@class, 'CardInfo')]//a"));

        for (WebElement item : goods) {
            String link = item.getAttribute("href");
            links.add(link);
        }
    }

    private void collectNomenclatures() throws IOException {
        int nomenclatureCost = 5000 / links.size();

        for (String link : links) {
            Document document = Jsoup.connect(link)
                    .userAgent(getUserAgent())
                    .timeout(5000)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .referrer(Const.REFERRER)
                    .get();

            Element element = Objects.requireNonNull(document.body().getElementById("j-item-buyzone")).child(1);

            String id = element.getElementsByAttribute("data-code").get(0).attr("data-code");
            String name = element.getElementsByAttribute("data-code").get(0).attr("data-name");
            String producer = element.getElementsByAttribute("data-code").get(0).attr("data-producer_name");
            String categoryName = element.getElementsByAttribute("data-code").get(0).attr("data-category");
            String oldPrice = element.getElementsByAttribute("data-code").get(0).attr("data-old_price");
            String price = element.getElementsByAttribute("data-code").get(0).attr("data-price");

            nomenclatures.add(new Nomenclature(Long.parseLong(id), name, producer, categoryName, oldPrice, price, link));

            currentProgressValue += nomenclatureCost;
            updateMessage("Сбор данных по " + name);
            updateProgress(currentProgressValue, 10000);
        }
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

    private void createExcel() {
        updateMessage("Создание excel файла...");
        updateProgress(9100, 10000);
        int startRow = 1;
        int startColumn = 1;

        //Создаю книгу excel
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
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
            rrcHead.setCellValue("Цена продажи, руб.");
            headStyle.setWrapText(true);
            rrcHead.setCellStyle(headStyle);

            XSSFCell deviationHead = head.createCell(++startColumn);
            deviationHead.setCellValue("Отклонение от цены продажи, %.");
            headStyle.setWrapText(true);
            deviationHead.setCellStyle(headStyle);

            sheet.setAutoFilter(CellRangeAddress.valueOf("B2:I2"));
            sheet.createFreezePane(0, 2);

            updateProgress(9300, 10000);

            int startRowContent = startRow + 1;

            int costNomenclature = 700 / nomenclatures.size();

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
                if (!Objects.equals(nom.oldPrice(), "")) {
                    oldPrice.setCellValue(Double.parseDouble(nom.oldPrice()));
                }
                oldPrice.setCellStyle(contentDecimalStyle);
                sheet.setColumnWidth(startColumnContent, 3808);

                double sellPriceFromExcel = getSellPriceFromExcel(nom.id());

                //РРЦ
                XSSFCell sellPrice = content.createCell(++startColumnContent);
                contentStyle.setWrapText(true);
                sellPrice.setCellValue(sellPriceFromExcel);
                sellPrice.setCellStyle(contentDecimalStyle);
                sheet.setColumnWidth(startColumnContent, 3808);

                //Отклонение
                XSSFCell deviation = content.createCell(++startColumnContent);
                contentStyle.setWrapText(true);
                if (sellPriceFromExcel > 0 && !nom.price().equals("0.0")) {
                    deviation.setCellValue((Double.parseDouble(nom.price()) / sellPriceFromExcel - 1) * 100);
                }
                deviation.setCellStyle(contentDecimalStyle);
                sheet.setColumnWidth(startColumnContent, 3808);

                currentProgressValue += costNomenclature;
                updateProgress(currentProgressValue, 10000);

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

    private double getSellPriceFromExcel(long id) throws IOException {
        double sellPrice = 0d;
        int rowStart = getNumberRowOrColumn(Const.ROW_START) == -1? 4 : getNumberRowOrColumn(Const.ROW_START);
        int column = getNumberRowOrColumn(Const.COLUMN_START) == -1? 16 : getNumberRowOrColumn(Const.COLUMN_START);
        int columnPrice = getNumberRowOrColumn(Const.COLUMN_PRICE) == -1? 13 : getNumberRowOrColumn(Const.COLUMN_PRICE);

        XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(fileRRCPath));
        XSSFSheet sheet = workbook.getSheet("Лист1");
        for (int i = rowStart; i < sheet.getLastRowNum() - 1; i++) {
            XSSFRow row = sheet.getRow(i);

            if (row.getCell(column) != null) {
                if (row.getCell(column).getNumericCellValue() == id) {
                    sellPrice = row.getCell(columnPrice).getNumericCellValue();
                    break;
                }
            }
            rowStart++;
        }

        workbook.close();
        return sellPrice;
    }

    private int getNumberRowOrColumn(String param) throws IOException {
        int result = -1;
        BufferedReader reader = new BufferedReader(new FileReader(Const.CONFIG_FILE_NAME));
        String line = reader.readLine();

        while (line != null) {
            if (line.contains("=")) {
                String trimmed = line.substring(line.indexOf("=") + 1).trim();
                if (line.startsWith(param)) {
                    result = Integer.parseInt(trimmed) - 1;
                    break;
                }
            }
            line = reader.readLine();
        }
        reader.close();

        return result;
    }
}
