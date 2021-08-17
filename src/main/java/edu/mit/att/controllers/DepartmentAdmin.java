package edu.mit.att.controllers;

import edu.mit.att.authz.Role;
import edu.mit.att.entity.Department;
import edu.mit.att.entity.SubmissionAgreement;
import edu.mit.att.entity.User;
import edu.mit.att.repository.DepartmentRepository;
import edu.mit.att.repository.SubmissionAgreementRepository;
import edu.mit.att.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

@Controller
public class DepartmentAdmin {

    private final org.slf4j.Logger LOGGER = getLogger(this.getClass());

    @Autowired
    private UserRepository userrepo;

    @Autowired
    private DepartmentRepository departmentrepo;

    @Autowired
    private SubmissionAgreementRepository ssarepo;

    @Resource
    private Environment env;

    // ------------------------------------------------------------------------
    @RequestMapping(value = "/AddDepartment", method = RequestMethod.GET)
    public ModelAndView AddDepartment(
            ModelMap model1, HttpServletRequest request) {
        LOGGER.info( "AddDepartment GET");

        final Department item = new Department();

        final String userAttrib = (String) request.getHeader("mail");
        final User user = userrepo.findByEmail(userAttrib).get(0);

        if (!user.getRole().equals(Role.siteadmin.name())) {
            return new ModelAndView("Permissions");
        }

        final ModelAndView model = new ModelAndView("AddDepartment");
        model.addObject("department", item);
        return model;
    }


    // ------------------------------------------------------------------------
    @RequestMapping(value = "/AddDepartment", method = RequestMethod.POST)
    public String AddDepartment(
            ModelMap model,
            @RequestParam("name") String departmentname,
            HttpSession session) {
        LOGGER.info( "AddDepartment Post");

        LOGGER.info("Saving:{}", departmentname);

       /* Utils utils = new Utils();
        if (!utils.setupAdminHandler(model, session, env)) {
            return "Home";
        }*/

        Department df = new Department();
        df.setName(departmentname);
        departmentrepo.save(df);

        LOGGER.debug("Saved:{}", departmentrepo.findAll());

        List<Department> departments = departmentrepo.findAllOrderByNameAsc();
        model.addAttribute("department", departments);

        return "ListDepartments";
    }

    // ------------------------------------------------------------------------    
    @RequestMapping(value = "/ListDepartments", method = RequestMethod.GET)
    public String ListDepartments(
            ModelMap model,
            HttpSession session
    ) {
        LOGGER.info( "ListDepartments Get");

        List<Department> departments = departmentrepo.findAllOrderByNameAsc();
        model.addAttribute("department", departments);

        return "ListDepartments";
    }

    // ------------------------------------------------------------------------
    @RequestMapping(value = "/EditDepartment", method = RequestMethod.GET)
    public String EditDepartment(
            ModelMap model,
            @RequestParam("departmentid") int departmentid,
            HttpSession session,
            HttpServletRequest request
    ) {
        LOGGER.info( "EditDepartment Get");

        final String userAttrib = (String) request.getHeader("mail");
        final User user = userrepo.findByEmail(userAttrib).get(0);

        final Set<Department> departments = user.getDepartments();


        if (departmentid <= 0) {
            LOGGER.error("departmentid <= 0");
            model.addAttribute("departmentiderror", "1");
            return "ListDepartments";
        }

        final Department departmentToEdit = departmentrepo.findById(departmentid);

        if (!departments.contains(departmentToEdit)) {
            return "Permissions";
        }

        model.addAttribute("department", departmentToEdit);

        return "EditDepartment";
    }

    // ------------------------------------------------------------------------
    @RequestMapping(value = "/EditDepartment", method = RequestMethod.POST)
    public String EditDepartment(
            Department department,
            BindingResult result,
            final RedirectAttributes redirectAttributes,
            ModelMap model,
            HttpSession session
    ) {
        LOGGER.info( "EditDepartment Post");

        LOGGER.info("Object:{}", department.toString());

       /* Utils utils = new Utils();
        if (!utils.setupAdminHandler(model, session, env)) {
            return "Home";
        }
*/
        department = departmentrepo.save(department);


        LOGGER.info("Saved:{}", departmentrepo.findAll().toString());
        LOGGER.info("Saved object:{}", department);


        model.addAttribute("department", department);
        model.addAttribute("departmentid", department.getId());

        List<User> users = department.getUsers();

        List<SubmissionAgreement> ssas = ssarepo.findAllForDepartmentId(department.getId());

        if (ssas == null) {
            ssas = Collections.emptyList();
        }

        if (users == null) {
            users = Collections.emptyList();
        }

        model.addAttribute("ssas", ssas);

        if (ssas.size() > 0 || users.size() > 0) {
            model.addAttribute("dependencies", "1");
        } else {
            model.addAttribute("dependencies", "0");
        }

        return "redirect:/ListDepartments";
    }

}
