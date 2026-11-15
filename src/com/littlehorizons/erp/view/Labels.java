package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.enums.DocumentType;
import com.littlehorizons.erp.model.enums.Gender;

/**
 * Friendly names shown in the menus for some enum values.
 */
public final class Labels {

    private Labels() {
    }

    public static String documentName(DocumentType type) {
        switch (type) {
            case CIVIL_REGISTRY:
                return "RC - Registro Civil";
            case IDENTITY_CARD:
                return "TI - Tarjeta de Identidad";
            case CITIZENSHIP_CARD:
                return "CC - Cédula de Ciudadanía";
            case FOREIGNER_ID:
                return "CE - Cédula de Extranjería";
            default:
                return "Pasaporte";
        }
    }

    public static String genderName(Gender gender) {
        switch (gender) {
            case MALE:
                return "Masculino";
            case FEMALE:
                return "Femenino";
            default:
                return "Otro";
        }
    }
}
