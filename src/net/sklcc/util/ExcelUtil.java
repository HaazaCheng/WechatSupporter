package net.sklcc.util;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hazza on 2017/7/13.
 */
public class ExcelUtil {
    private ExcelUtil() {};

    public static List<String> readAccountsFromExcel(String filePath) {
        List<String> accounts = new ArrayList<>();
        Workbook wb = null;

        try (FileInputStream in = new FileInputStream(filePath)){
            wb = Workbook.getWorkbook(in);
            Sheet sheet = wb.getSheet(0);
            int columns = sheet.getColumns(), rows = sheet.getRows();
            System.out.println("There are " + rows + " rows and " + columns +" columns.");
            for (int i = 0; i < rows; i++) {
                Cell cell = sheet.getCell(1, i);
                accounts.add((String)cell.getContents());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            wb.close();
        }

        return accounts;
    }

    public static void main(String[] args) {
        List<String> accounts = ExcelUtil.readAccountsFromExcel("C:\\Users\\Krystal\\Desktop\\accounts.xls");
        for (String account : accounts) {
            System.out.println(account);
        }
    }
}
