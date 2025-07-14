package com.znt.outbound.model.jit;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JitAsnLineAttr {
    @JsonProperty("Loc")
    private String loc;

    @JsonProperty("Lpn")
    private String lpn;

    @JsonProperty("DateCode")
    private String dateCode;

    @JsonProperty("ExpiredDt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private String expiredDt;

    @JsonProperty("Coo")
    private String coo;

    @JsonProperty("PackageType")
    private String packageType;

    @JsonProperty("VLot")
    private String vLotValue;

    @JsonProperty("LotAttr01")
    private String lotAttr01;

    @JsonProperty("LotAttr02")
    private String lotAttr02;

    @JsonProperty("LotAttr03")
    private String lotAttr03;

    @JsonProperty("LotAttr04")
    private String lotAttr04;

    @JsonProperty("LotAttr05")
    private String lotAttr05;

    @JsonProperty("LotAttr06")
    private String lotAttr06;

    @JsonProperty("LotAttr07")
    private String lotAttr07;

    @JsonProperty("LotAttr08")
    private String lotAttr08;

    @JsonProperty("LotAttr09")
    private String lotAttr09;

    @JsonProperty("LotAttr10")
    private String lotAttr10;

    @JsonProperty("LotAttr11")
    private String lotAttr11;

    @JsonProperty("LotAttr12")
    private String lotAttr12;

    @JsonProperty("LotAttr13")
    private String lotAttr13;

    @JsonProperty("LotAttr14")
    private String lotAttr14;

    @JsonProperty("LotAttr15")
    private String lotAttr15;

    @JsonProperty("LotAttr16")
    private String lotAttr16;

    @JsonProperty("LotAttr17")
    private String lotAttr17;

    @JsonProperty("LotAttr18")
    private String lotAttr18;

    @JsonProperty("LotAttr19")
    private String lotAttr19;

    @JsonProperty("LotAttr20")
    private String lotAttr20;

    // Getter and Setter methods
    public String getLoc() { return loc; }
    public void setLoc(String loc) { this.loc = loc; }

    public String getLpn() { return lpn; }
    public void setLpn(String lpn) { this.lpn = lpn; }

    public String getDateCode() { return dateCode; }
    public void setDateCode(String dateCode) { this.dateCode = dateCode; }

    public String getExpiredDt() { return expiredDt; }
    public void setExpiredDt(String expiredDt) { this.expiredDt = expiredDt; }

    public String getCoo() { return coo; }
    public void setCoo(String coo) { this.coo = coo; }

    public String getPackageType() { return packageType; }
    public void setPackageType(String packageType) { this.packageType = packageType; }

    @JsonIgnore
    public String getVLotValue() { return vLotValue; }
    public void setVLotValue(String vLotValue) { this.vLotValue = vLotValue; }

    public String getLotAttr01() { return lotAttr01; }
    public void setLotAttr01(String lotAttr01) { this.lotAttr01 = lotAttr01; }

    public String getLotAttr02() { return lotAttr02; }
    public void setLotAttr02(String lotAttr02) { this.lotAttr02 = lotAttr02; }

    public String getLotAttr03() { return lotAttr03; }
    public void setLotAttr03(String lotAttr03) { this.lotAttr03 = lotAttr03; }

    public String getLotAttr04() { return lotAttr04; }
    public void setLotAttr04(String lotAttr04) { this.lotAttr04 = lotAttr04; }

    public String getLotAttr05() { return lotAttr05; }
    public void setLotAttr05(String lotAttr05) { this.lotAttr05 = lotAttr05; }

    public String getLotAttr06() { return lotAttr06; }
    public void setLotAttr06(String lotAttr06) { this.lotAttr06 = lotAttr06; }

    public String getLotAttr07() { return lotAttr07; }
    public void setLotAttr07(String lotAttr07) { this.lotAttr07 = lotAttr07; }

    public String getLotAttr08() { return lotAttr08; }
    public void setLotAttr08(String lotAttr08) { this.lotAttr08 = lotAttr08; }

    public String getLotAttr09() { return lotAttr09; }
    public void setLotAttr09(String lotAttr09) { this.lotAttr09 = lotAttr09; }

    public String getLotAttr10() { return lotAttr10; }
    public void setLotAttr10(String lotAttr10) { this.lotAttr10 = lotAttr10; }

    public String getLotAttr11() { return lotAttr11; }
    public void setLotAttr11(String lotAttr11) { this.lotAttr11 = lotAttr11; }

    public String getLotAttr12() { return lotAttr12; }
    public void setLotAttr12(String lotAttr12) { this.lotAttr12 = lotAttr12; }

    public String getLotAttr13() { return lotAttr13; }
    public void setLotAttr13(String lotAttr13) { this.lotAttr13 = lotAttr13; }

    public String getLotAttr14() { return lotAttr14; }
    public void setLotAttr14(String lotAttr14) { this.lotAttr14 = lotAttr14; }

    public String getLotAttr15() { return lotAttr15; }
    public void setLotAttr15(String lotAttr15) { this.lotAttr15 = lotAttr15; }

    public String getLotAttr16() { return lotAttr16; }
    public void setLotAttr16(String lotAttr16) { this.lotAttr16 = lotAttr16; }

    public String getLotAttr17() { return lotAttr17; }
    public void setLotAttr17(String lotAttr17) { this.lotAttr17 = lotAttr17; }

    public String getLotAttr18() { return lotAttr18; }
    public void setLotAttr18(String lotAttr18) { this.lotAttr18 = lotAttr18; }

    public String getLotAttr19() { return lotAttr19; }
    public void setLotAttr19(String lotAttr19) { this.lotAttr19 = lotAttr19; }

    public String getLotAttr20() { return lotAttr20; }
    public void setLotAttr20(String lotAttr20) { this.lotAttr20 = lotAttr20; }
}