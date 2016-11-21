package pivotal.au.apples.demo.controller;

import pivotal.au.apples.demo.repository.EmployeeRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import pivotal.au.apples.demo.util.Utils;

@Controller
public class EmployeeController
{
    private static Log logger = LogFactory.getLog(EmployeeController.class);

    private EmployeeRepository employeeRepository;

    @Autowired
    public EmployeeController(EmployeeRepository employeeRepository)
    {
        this.employeeRepository = employeeRepository;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String homePage(Model model) throws Exception
    {
        model.addAttribute("employees", employeeRepository.findAll());
        model.addAttribute("appIndex", Utils.applicationIndex());
        model.addAttribute("dbservice", Utils.getDBService());

        return "employees";
    }
}
