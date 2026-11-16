package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.AcademicController;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Subject;

import java.util.List;

/** Reusable selection dialogs (grade and subject) shared by several menus. */
public final class Pickers {

    private Pickers() {
    }

    public static Grade chooseGrade(InputReader input, AcademicController academic) throws ErpException {
        return input.readChoice("Grado:", academic.listGrades(), Grade::getName);
    }

    public static Subject chooseSubject(InputReader input, AcademicController academic) throws ErpException {
        Grade grade = chooseGrade(input, academic);
        List<Subject> subjects = academic.listSubjectsOfGrade(grade.getGradeId());
        return input.readChoice("Materia:", subjects, s -> s.getName() + " (" + s.getCode() + ")");
    }
}