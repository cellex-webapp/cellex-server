package com.example.cellex.dtos.request.review;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class VendorResponseRequest {

    @NotBlank(message = "Nội dung phản hồi không được để trống")
    private String comment;

    public VendorResponseRequest() {
    }

    @JsonCreator
    public VendorResponseRequest(@JsonProperty("comment") String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    @JsonProperty("comment")
    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String toString() {
        return "VendorResponseRequest{comment='" + comment + "'}";
    }
}
