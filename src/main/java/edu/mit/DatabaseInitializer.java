package edu.mit;

import edu.mit.api.Departments;
import edu.mit.entity.DepartmentsForm;
import edu.mit.entity.SsasForm;
import edu.mit.entity.UsersForm;
import edu.mit.repository.DepartmentsFormRepository;
import edu.mit.repository.SsasFormRepository;
import edu.mit.repository.UsersFormRepository;
import edu.mit.service.SsasFormService;
import edu.mit.service.UsersFormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class DatabaseInitializer {

    private final Logger logger = getLogger(this.getClass());

    @Autowired
    public DatabaseInitializer() {
    }

    @Autowired
    private DepartmentsFormRepository departmentrepo;

    @Autowired
    private UsersFormService usersFormService;

    @Autowired
    private SsasFormService ssasFormService;

    /**
     * Populates the database
     */
    @PostConstruct
    public void populateDatabase() {

        logger.info("INIT");
        logger.debug("(Initialize database here if you want to. . .)");

        //FIXME this is only for testing purposes. remvoe later.

        DepartmentsForm departmentsForm = new DepartmentsForm();
        departmentsForm.setName("IASC");



        try {
            departmentrepo.save(departmentsForm);


            UsersForm usersForm = new UsersForm();
            usersForm.setUsername("testuser");
            usersForm.setEmail("test@mit.edu");
            usersForm.setFirstname("K");
            usersForm.setLastname("Smith");
            usersForm.setIsadmin(true);
            List<DepartmentsForm> depts = departmentrepo.findAll();

            // done to take care of hiberante exception

            List<DepartmentsForm> d = new ArrayList<>();
            for (DepartmentsForm dept : depts) {
                d.add(dept);
            }
            //usersForm.setDepartmentsForms(d);
            usersFormService.create(usersForm, depts);

            SsasForm ssasForm = new SsasForm();
            ssasForm.setId(1);
            ssasForm.setApproved(true);
            ssasForm.setSsaCopyrightsForms(Collections.emptyList());
            ssasForm.setSsaAccessRestrictionsForms(Collections.emptyList());
            ssasForm.setSsaContactsForms(Collections.emptyList());
            ssasForm.setSsaFormatTypesForms(Collections.emptyList());
            List<DepartmentsForm> departmentsForms = new ArrayList<>();
            ssasFormService.create(ssasForm, departmentsForm, null, null);

            ssasForm.setApproved(true);
            ssasForm.setSsaCopyrightsForms(Collections.emptyList());
            ssasForm.setSsaAccessRestrictionsForms(Collections.emptyList());
            ssasForm.setSsaContactsForms(Collections.emptyList());
            ssasForm.setSsaFormatTypesForms(Collections.emptyList());
            ssasFormService.saveFormTest(ssasForm, null);


        } catch (Exception e) {
            logger.error("Error saving test item:{}", e);
        }

        logger.debug("Saved user:{}", usersFormService.findAll());


    }

}
