package com.github.tobiasmiosczka.gas_exporter.model.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class Station {

    private UUID id;
    private String name;
    private String brand;
    private String street;
    private String place;
    private Double lat;
    private Double lng;
    private Double dist;
    private BigDecimal diesel;
    private BigDecimal e5;
    private BigDecimal e10;
    private Boolean isOpen;
    private String houseNumber;
    private Integer postCode;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public Double getDist() {
        return dist;
    }

    public void setDist(Double dist) {
        this.dist = dist;
    }

    public BigDecimal getDiesel() {
        return diesel;
    }

    public void setDiesel(BigDecimal diesel) {
        this.diesel = diesel;
    }

    public BigDecimal getE5() {
        return e5;
    }

    public void setE5(BigDecimal e5) {
        this.e5 = e5;
    }

    public BigDecimal getE10() {
        return e10;
    }

    public void setE10(BigDecimal e10) {
        this.e10 = e10;
    }

    public Boolean isOpen() {
        return isOpen;
    }

    public void setOpen(Boolean open) {
        isOpen = open;
    }

    public String getHouseNumber() {
        return houseNumber;
    }

    public void setHouseNumber(String houseNumber) {
        this.houseNumber = houseNumber;
    }

    public Integer getPostCode() {
        return postCode;
    }

    public void setPostCode(Integer postCode) {
        this.postCode = postCode;
    }
}