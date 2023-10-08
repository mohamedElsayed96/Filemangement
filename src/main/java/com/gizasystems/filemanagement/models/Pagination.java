package com.gizasystems.filemanagement.models;

import lombok.Getter;
import lombok.Setter;

/**
 * pagination class
 */

public class Pagination {

    /**
     * total count property
     */
    @Getter
    @Setter
    private long total = 0;
    /**
     * page size property
     */
    @Getter
    @Setter
    private int pageSize = 5;
    /**
     * page number property
     */
    @Getter
    @Setter
    private int pageNum = 0;

    /**
     * pagination constructor
     */

    public Pagination() {
        super();
    }

    /**
     * pagination constructor
     */

    public Pagination(final int pageSize, final int pageNum) {
        super();
        this.pageSize = pageSize;
        this.pageNum = pageNum;
    }

    /**
     * pagination constructor
     */

    public Pagination(final long total, final int pageSize, final int pageNum) {
        super();
        this.total = total;
        this.pageSize = pageSize;
        this.pageNum = pageNum;
    }

    @Override
    public String toString() {
        return "Pagination{" +
                "total=" + total +
                ", pageSize=" + pageSize +
                ", pageNum=" + pageNum +
                '}';
    }
}
