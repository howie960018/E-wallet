package com.example.wallet.common.result;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {

    /**
     * 當前頁資料
     */
    private List<T> records;

    /**
     * 總筆數
     */
    private Long total;

    /**
     * 當前頁碼
     */
    private Integer pageNum;

    /**
     * 每頁筆數
     */
    private Integer pageSize;

    /**
     * 總頁數
     */
    private Integer totalPages;

    public static <T> PageResult<T> of(List<T> records, Long total,
                                       Integer pageNum, Integer pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setTotalPages((int) Math.ceil((double) total / pageSize));
        return result;
    }
}