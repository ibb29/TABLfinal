package com.example.android.tabl.basket_checkout_recyclerview;

import android.content.Context;
import android.content.res.Resources;

import com.example.android.tabl.R;
import com.google.android.gms.maps.model.LatLng;

public class BasketCheckout {

    private String itemName;
    private String itemId;
    private String price;
    private int quantity;
    private String option;

    public BasketCheckout() {
    }

    //unfinished default constructor. Passing context is bad and I shouldn't do it.
    public BasketCheckout(Context c) {
        Resources res = c.getResources();
        this.itemName = res.getString(R.string.menu_item);
        this.price = res.getString(R.string.menu_price);
        this.itemId = res.getString(R.string.itemID);
        this.quantity = res.getInteger(R.integer.default_quantity);
        this.option = res.getString(R.string.default_options);
        //this.menuIds = res.getStringArray(R.array.menu_ids);
    }

    public BasketCheckout(String itemName, String itemId, String option, String price, int quantity) {
        this.itemName = itemName;
        this.itemId = itemId;
        this.price = price;
        this.quantity = quantity;
        this.option = option;
    }

    public String getName() {
        return itemName;
    }

    public void setName(String itemName) {
        this.itemName = itemName;
    }

    public String getId() {
        return itemId;
    }

    public void setId(String itemId) {
        this.itemId = itemId;
    }

    public String getPrice() {
        return this.price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public int setQuantity(String id) {
        return quantity;
    }

    public String getOptions() {
        return option;
    }

    public String setOptions(String id) {
        return option;
    }

}