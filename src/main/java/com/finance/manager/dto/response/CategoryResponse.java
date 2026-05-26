package com.finance.manager.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.finance.manager.model.TransactionType;

public class CategoryResponse {

    private String name;
    private TransactionType type;
    @JsonProperty("isCustom")
    private boolean isCustom;

    public CategoryResponse() {}

    public CategoryResponse(String name, TransactionType type, boolean isCustom) {
        this.name = name;
        this.type = type;
        this.isCustom = isCustom;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    @JsonProperty("isCustom")
    public boolean isCustom() { return isCustom; }
    public void setCustom(boolean custom) { isCustom = custom; }
}
