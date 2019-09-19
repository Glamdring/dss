package eu.europa.esig.dss.model;

import java.io.Serializable;
import java.util.List;

@SuppressWarnings("serial")
public class SignatureImagePageRange implements Serializable {
    private boolean all;
    private List<Integer> pages;
    private boolean excludeLast;
    private boolean excludeFirst;
    private int excludeLastCount;
    private int excludeFirstCount;

    public boolean isAll() {
        return all;
    }
    public void setAll(boolean all) {
        this.all = all;
    }

    public List<Integer> getPages() {
        return pages;
    }
    public void setPages(List<Integer> pages) {
        this.pages = pages;
    }
    public boolean isExcludeLast() {
        return excludeLast;
    }
    public void setExcludeLast(boolean excludeLast) {
        this.excludeLast = excludeLast;
    }
    public boolean isExcludeFirst() {
        return excludeFirst;
    }
    public void setExcludeFirst(boolean excludeFirst) {
        this.excludeFirst = excludeFirst;
    }
    public int getExcludeLastCount() {
        return excludeLastCount;
    }
    public void setExcludeLastCount(int excludeLastCount) {
        this.excludeLastCount = excludeLastCount;
    }
    public int getExcludeFirstCount() {
        return excludeFirstCount;
    }
    public void setExcludeFirstCount(int excludeFirstCount) {
        this.excludeFirstCount = excludeFirstCount;
    }
}