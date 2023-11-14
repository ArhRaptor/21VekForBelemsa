package by.parser.parser21vekbelemsa;

public class Const {
    public static final String REFERRER = "https://www.google.com/search?q=21+%D0%B2%D0%B5%D0%BA+%D1%81%D0%B0%D0%B9%D1%82&oq=21&gs_lcrp=EgZjaHJvbWUqCwgBEEUYJxg7GIoFMgsIABBFGCcYORiKBTILCAEQRRgnGDsYigUyDQgCEAAYgwEYsQMYgAQyDQgDEAAYgwEYsQMYgAQyEwgEEC4YgwEYrwEYxwEYsQMYgAQyDQgFEAAYgwEYsQMYgAQyDQgGEAAYgwEYsQMYigUyBwgHEAAYgAQyBwgIEAAYgAQyBwgJEAAYjwLSAQkyNDgwajBqMTWoAgCwAgA&sourceid=chrome&ie=UTF-8";
    public static final String CONFIG_FILE_NAME = "config.ini";
    public static final String USER_AGENT = "#userAgent";
    public static final String CATEGORIES = "#categories";
    public static final String SENSO = "#senso";
    public static final String SENSO_BABY = "#babySenso";
    public static final String SENSO_MED = "#medSenso";
    public static final String NIHON_BABY = "#nihonBaby";
    public static final String MIU = "#miu";
    public static final String DEFAULT_INI = """
            *********************************07.10.2023***********************************************
            Файл с настройками для парсинга сайта 21vek.by

            Все параметры начинаются с символа #. Без этого работать не будет!!!

            В данном файле:
            -userAgent - userAgen, который можно взять, например, на сайте: https://www.whatismybrowser.com/guides/the-latest-user-agent/. Данные брать путем копирования самого последнего (свежего) user-agent в сучае, когда программа перестала работать (иногда это может решить проблему).
            -categories - перечень категорий для вставки в адресную строку.  Например, diapers (подгузники) или underpads (пеленки).
            -senso, babySenso, medSenso, nihonBaby, miu - в данных параметрах пишется наименование брендов, которые нужно вставить в качестве фильта в адресную строку. Например, sensoBaby = senso_baby.

            *********************************Настройки************************************************

            #userAgent = Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36 Edg/119.0.2151.58

            ******************************************************************************************

            ***********************************Бренды*************************************************

            #senso = senso
            #babySenso = senso_baby
            #nihonBaby = nihon_baby
            #miu = miu

            ******************************************************************************************

            *********************************Категории************************************************

            #categories = diapers, underpads, adult_diapers

            ******************************************************************************************""";
}
