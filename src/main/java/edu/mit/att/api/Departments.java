package edu.mit.att.api;


import edu.mit.att.entity.DepartmentsForm;
import edu.mit.att.repository.DepartmentsFormRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class Departments {

    @Autowired
    private DepartmentsFormRepository repo;

    @RequestMapping("/departments")
    public DepartmentsForm getusers(@RequestParam(value = "name", defaultValue = "test") String name) {
        final List<DepartmentsForm> departmentList = repo.findByName(name);
        if (departmentList.isEmpty()) {
            return new DepartmentsForm();
        }
        return departmentList.get(0);
    }

    @RequestMapping("/departments/all")
    public List<DepartmentsForm> getDepartmentsAll() {
        final List<DepartmentsForm> departmentList = repo.findAll();
        return departmentList;
    }
}