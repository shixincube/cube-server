/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc;

/**
 * 咨询主题。
 */
public enum ConsultationTheme {

    EmotionalStress("ES", "情绪压力", "Emotional Stress"),

    FamilyAndMaritalRelationships("FMR", "家庭婚恋", "Family and Marital Relationships"),

    SexualPsychology("SP", "性心理", "Sexual Psychology"),

    InterpersonalRelationship("IR", "人际关系", "Interpersonal Relationship"),

    PersonalGrowth("PG", "个人成长", "Personal Growth"),

    AcademicAndCareerStudies("ACS", "学业职场", "Academic and Career Studies"),

    ParentalUpbringing("PU", "亲子教育", "Parental Upbringing"),

    MentalHealth("MH", "心理健康", "Mental Health"),

    CareerGuidance("CG", "生涯指导", "Career Guidance"),

    ;

    public final String code;

    public final String nameCN;

    public final String nameEN;

    ConsultationTheme(String code, String nameCN, String nameEN) {
        this.code = code;
        this.nameCN = nameCN;
        this.nameEN = nameEN;
    }

    public static ConsultationTheme parse(String nameOrCode) {
        for (ConsultationTheme theme : ConsultationTheme.values()) {
            if (theme.name().equalsIgnoreCase(nameOrCode) || theme.code.equalsIgnoreCase(nameOrCode) ||
                theme.nameCN.equals(nameOrCode) || theme.nameEN.equalsIgnoreCase(nameOrCode)) {
                return theme;
            }
        }
        return null;
    }
}
