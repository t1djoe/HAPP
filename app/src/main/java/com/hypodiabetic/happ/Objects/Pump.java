package com.hypodiabetic.happ.Objects;

import com.crashlytics.android.Crashlytics;
import com.hypodiabetic.happ.Constants;
import com.hypodiabetic.happ.tools;

import java.util.Date;

/**
 * Created by Tim on 16/02/2016.
 */
public class Pump {

    public String name;                         //name of pump
    public Integer basal_mode;                  //Basal adjustment mode
    public Integer min_low_basal_duration;      //low basal duration supported
    public Integer min_high_basal_duration;     //low basal duration supported
    public Double default_basal_rate;           //What is the current default rate
    public Boolean temp_basal_active=false;     //Is a temp basal active
    public Double temp_basal_rate;              //Current temp basal rate
    public Integer temp_basal_percent;          //Current temp basal percent
    public Integer temp_basal_duration;         //Temp duration in Mins
    public Long temp_basal_duration_left;       //Mins left of this Temp Basal

    private Profile profile        =   new Profile(new Date());
    private TempBasal tempBasal    =   TempBasal.last();


    private static final int ABSOLUTE               =  1;       //Absolute (U/hr)  Percent of Basal
    private static final int PERCENT                =  2;       //Percent of Basal
    private static final int BASAL_PLUS_PERCENT     =  3;       //hourly basal rate plus TBR percentage


    public Pump(){

        name                =   profile.pump_name;
        default_basal_rate  =   profile.current_basal;

        switch (name){
            case "roche_combo":
                basal_mode              =   BASAL_PLUS_PERCENT;
                min_low_basal_duration  =   30;
                min_high_basal_duration =   30;
                break;

            case "dana_r":
                basal_mode              =   BASAL_PLUS_PERCENT;
                min_low_basal_duration  =   60;
                min_high_basal_duration =   30;
                break;

            case "medtronic_absolute":
                basal_mode              =   ABSOLUTE;
                min_low_basal_duration  =   30;
                min_high_basal_duration =   30;
                break;

            case "medtronic_percent":
                basal_mode              =   PERCENT;
                min_low_basal_duration  =   30;
                min_high_basal_duration =   30;
                break;
        }

        temp_basal_active   =   tempBasal.isactive(new Date());
        if (temp_basal_active){
            temp_basal_rate             =   tempBasal.rate;
            temp_basal_percent          =   getBasalPercent();
            temp_basal_duration         =   tempBasal.duration;
            temp_basal_duration_left    =   tempBasal.durationLeft();
        }
    }

    public void setNewTempBasal(APSResult apsResult, TempBasal tempBasal){
        if (apsResult != null){
            temp_basal_rate             =   apsResult.rate;
            temp_basal_duration         =   apsResult.duration;
            temp_basal_duration_left    =   apsResult.duration.longValue();
        } else {
            temp_basal_rate             =   tempBasal.rate;
            temp_basal_duration         =   tempBasal.duration;
            temp_basal_duration_left    =   tempBasal.durationLeft();
        }
        temp_basal_active   =   true;
        temp_basal_percent  =   getBasalPercent();
    }

    public String displayCurrentBasal(boolean small){
        if (small) {
            switch (basal_mode) {
                case ABSOLUTE:
                    return tools.formatDisplayBasal(activeRate(), false);
                case PERCENT:
                    return calcPercentOfBasal() + "%";
                case BASAL_PLUS_PERCENT:
                    return calcBasalPlusPercent() + "%";
            }
        } else {
            switch (basal_mode) {
                case ABSOLUTE:
                    return tools.formatDisplayBasal(activeRate(), false);
                case PERCENT:
                    return calcPercentOfBasal() + "% (" + tools.formatDisplayBasal(activeRate(), false) + ")";
                case BASAL_PLUS_PERCENT:
                    return calcBasalPlusPercent() + "% (" + tools.formatDisplayBasal(activeRate(), false) + ")";
            }
        }
        Crashlytics.log(1,"APSService","Could not get displayCurrentBasal: " + basal_mode + " " + name);
        return "error";
    }

    public String displayTempBasalMinsLeft(){
        if (temp_basal_active){
            if (temp_basal_duration_left > 1){
                return temp_basal_duration_left + " mins";
            } else {
                return temp_basal_duration_left + " min";
            }
        } else {
            return "";
        }
    }

    public String displayBasalDesc(boolean small){
        if (small) {
            if (temp_basal_active) {
                if (temp_basal_rate > default_basal_rate) {
                    return Constants.ARROW_SINGLE_UP;
                } else {
                    return Constants.ARROW_SINGLE_DOWN;
                }
            } else {
                return "";
            }
        } else {
            if (temp_basal_active) {
                if (temp_basal_rate > default_basal_rate) {
                    return "High Temp";
                } else {
                    return "Low Temp";
                }
            } else {
                return "Default Basal";
            }
        }
    }

    private int getBasalPercent(){
        switch (basal_mode){
            case ABSOLUTE:
                return 0;
            case PERCENT:
                return calcPercentOfBasal();
            case BASAL_PLUS_PERCENT:
                return calcBasalPlusPercent();
        }
        Crashlytics.log(1,"APSService","Could not get getSuggestedBasalPercent: " + basal_mode + " " + name);
        return 0;
    }

    public Double activeRate(){
        if (temp_basal_active){
            return temp_basal_rate;
        } else {
            return default_basal_rate;
        }
    }


    private int calcPercentOfBasal(){
        Double ratePercent = (activeRate() - profile.current_basal);
        ratePercent = (ratePercent / activeRate()) *100;
        return ratePercent.intValue();
    }
    private int calcBasalPlusPercent(){
        Double ratePercent = (activeRate() / profile.current_basal) * 100;
        ratePercent = (double) Math.round(ratePercent / 10) * 10; //round to closest 10
        return ratePercent.intValue();
    }
}