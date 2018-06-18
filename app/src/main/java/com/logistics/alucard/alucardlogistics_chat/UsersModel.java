package com.logistics.alucard.alucardlogistics_chat;

public class UsersModel {

    private String name;
    private String device_token;
    private String status;
    private String image;
    private String thumb_image;
    private Boolean online;
    private Long last_seen;

    public UsersModel(String name, String device_token, String status, String image, String thumb_image, Boolean online, Long last_seen) {
        this.name = name;
        this.device_token = device_token;
        this.status = status;
        this.image = image;
        this.thumb_image = thumb_image;
        this.online = online;
        this.last_seen = last_seen;
    }

    public UsersModel() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getThumb_image() {
        return thumb_image;
    }

    public void setThumb_image(String thumb_image) {
        this.thumb_image = thumb_image;
    }

    public Boolean getOnline() {
        return online;
    }

    public void setOnline(Boolean online) {
        this.online = online;
    }

    public Long getLast_seen() {
        return last_seen;
    }

    public void setLast_seen(Long last_seen) {
        this.last_seen = last_seen;
    }

    public String getDevice_token() {
        return device_token;
    }

    public void setDevice_token(String device_token) {
        this.device_token = device_token;
    }

    @Override
    public String toString() {
        return "UsersModel{" +
                "name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", image='" + image + '\'' +
                ", thumb_image='" + thumb_image + '\'' +
                ", online=" + online +
                '}';
    }
}
