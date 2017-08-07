package net.sklcc.wechatsupporter.object;

/**
 * Created by hazza on 8/7/17.
 */
public class ArticleInfos {
    private String id = "";
    private String account = "";
    private String publish_time = "";
    private String title = "";
    private String summary = "";
    private String url = "";
    private String add_time = "";
    private String ranking = "";
    private String source_url = "";
    private String author = "";
    private String copyright = "";

    public ArticleInfos() {}

    public void setId(String id) {
        this.id = id;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public void setPublish_time(String publish_time) {
        this.publish_time = publish_time;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setAdd_time(String add_time) {
        this.add_time = add_time;
    }

    public void setRanking(String ranking) {
        this.ranking = ranking;
    }

    public void setSource_url(String source_url) {
        this.source_url = source_url;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public String getId() {
        return id;
    }

    public String getAccount() {
        return account;
    }

    public String getPublish_time() {
        return publish_time;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getUrl() {
        return url;
    }

    public String getAdd_time() {
        return add_time;
    }

    public String getRanking() {
        return ranking;
    }

    public String getSource_url() {
        return source_url;
    }

    public String getAuthor() {
        return author;
    }

    public String getCopyright() {
        return copyright;
    }

    @Override
    public String toString() {
        return "ArticleInfos{" +
                "id='" + id + '\'' +
                ", account='" + account + '\'' +
                ", publish_time='" + publish_time + '\'' +
                ", title='" + title + '\'' +
                ", summary='" + summary + '\'' +
                ", url='" + url + '\'' +
                ", add_time='" + add_time + '\'' +
                ", ranking='" + ranking + '\'' +
                ", source_url='" + source_url + '\'' +
                ", author='" + author + '\'' +
                ", copyright='" + copyright + '\'' +
                '}';
    }
}
