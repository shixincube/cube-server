package cube.service.tokenizer.keyword;

import java.math.BigDecimal;

public class Keyword implements Comparable<Keyword> {

    private double tfidfValue;

    private String word;

    /**
     * @return the TF-IDF Value
     */
    public double getTfidfValue() {
        return tfidfValue;
    }

    /**
     * @param tfidfValue the TF-IDF Value to set
     */
    public void setTfidfValue(double tfidfValue) {
        this.tfidfValue = tfidfValue;
    }

    /**
     * @return the word
     */
    public String getWord() {
        return word;
    }

    /**
     * @param word the word to set
     */
    public void setWord(String word) {
        this.word = word;
    }

    public Keyword(String word, double tfidfValue) {
        this.word = word;
        // TF-IDF 值只保留3位小数
        this.tfidfValue = (double) Math.round(tfidfValue * 10000) / 10000;
    }

    /**
     * 为了在返回 TD-IDF 分析结果时，可以按照值的从大到小顺序返回，故实现Comparable接口
     */
    @Override
    public int compareTo(Keyword other) {
        BigDecimal selfDec = new BigDecimal(Double.toString(this.tfidfValue));
        BigDecimal otherDec = new BigDecimal(Double.toString(other.tfidfValue));
        return  otherDec.compareTo(selfDec);
    }

    /**
     * 重写hashcode方法，计算方式与原生String的方法相同
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((word == null) ? 0 : word.hashCode());
        long temp;
        temp = Double.doubleToLongBits(tfidfValue);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        Keyword other = (Keyword) obj;
        if (word == null) {
            if (other.word != null)
                return false;
        } else if (!word.equals(other.word)) {
            return false;
        }
//		if (Double.doubleToLongBits(tfidfvalue) != Double.doubleToLongBits(other.tfidfvalue))
//			return false;
        return true;
    }
}
