package mn.edu.must;

import java.util.ArrayList;
import java.util.List;

public class SortNode {

    private final int left;
    private final int right;

    private String status;
    private String inputValues = "";
    private String sortedValues = "";

    private final List<SortNode> children = new ArrayList<>();

    public SortNode(int left, int right) {
        this.left = left;
        this.right = right;
        this.status = "Хүлээгдэж байна";
    }

    public int getLeft() {
        return left;
    }

    public int getRight() {
        return right;
    }

    public String getRange() {
        return "[" + left + "–" + right + "]";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInputValues() {
        return inputValues;
    }

    public void setInputValues(String inputValues) {
        this.inputValues = inputValues;
    }

    public String getSortedValues() {
        return sortedValues;
    }

    public void setSortedValues(String sortedValues) {
        this.sortedValues = sortedValues;
    }

    public List<SortNode> getChildren() {
        return children;
    }

    public void addChild(SortNode child) {
        children.add(child);
    }
}