package jp.jaxa.iss.kibo.rpc.SENSUAY_TEAM;

public class FinalAreaResult {
    private int areaId;
    private String itemName;
    private int itemCount;
    private float averageScore;

    public FinalAreaResult(int areaId, String itemName, int itemCount, float Score) {
        this.areaId = areaId;
        this.itemName = itemName;
        this.itemCount = itemCount;
        this.averageScore = Score;
    }

    // Getters
    public int getAreaId() { return areaId; }
    public String getItemName() { return itemName; }
    public int getItemCount() { return itemCount; }
    public float getAverageScore() { return averageScore; }

    public void setAreaId(int areaId) {
        this.areaId = areaId;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    public void setScore(float score) {
        averageScore = score;
    }

    @Override
    public String toString() {
        return "AreaResult{" +
                "area=" + areaId +
                ", item='" + itemName + '\'' +
                ", count=" + itemCount +
                ", avgScore=" + String.format("%.2f", averageScore) +
                '}';
    }

}
