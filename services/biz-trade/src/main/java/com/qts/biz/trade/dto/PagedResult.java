package com.qts.biz.trade.dto;

import java.util.List;

/**
 * Generic Paged Result DTO
 * Represents paginated query results
 */
public class PagedResult<T> {

    private List<T> data;
    private int page;
    private int pageSize;
    private long total;
    private boolean hasNext;

    public PagedResult() {}

    public PagedResult(List<T> data, int page, int pageSize, long total) {
        this.data = data;
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
        this.hasNext = (long) page * pageSize < total;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }
}
