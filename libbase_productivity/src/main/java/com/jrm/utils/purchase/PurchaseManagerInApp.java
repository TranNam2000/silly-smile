//package com.nomyek.utils.purchase;
//
//import com.nomyek.utils.BaseConstants;
//import com.nomyek.utils.SharedPref;
//
//public class PurchaseManagerInApp {
//    private static PurchaseManagerInApp instance;
//    public static PurchaseManagerInApp getInstance() {
//        if (instance == null) {
//            instance = new PurchaseManagerInApp();
//        }
//        return instance;
//    }
//    public boolean isPurchased() {
//        if (!SharedPref.readBoolean(BaseConstants.ENABLE_ADS, true)) {
//            return true;
//        }
//        return false;
//    }
//}
