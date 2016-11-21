package pivotal.au.apples.demo.controller;

import pivotal.au.apples.demo.domain.Employee;
import pivotal.au.apples.demo.repository.EmployeeRepository;
import pivotal.au.apples.demo.util.HeaderUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

@RestController
public class EmployeeRest
{
    private static Log logger = LogFactory.getLog(EmployeeRest.class);
    private EmployeeRepository employeeRepository;

    @Autowired
    public EmployeeRest(EmployeeRepository employeeRepository)
    {
        this.employeeRepository = employeeRepository;
    }

    @RequestMapping(value = "/emps",
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Employee> listEmployees()
    {
        logger.info("REST request to get all Employees");
        List<Employee> emps = employeeRepository.findAll();

        return emps;
    }

    @RequestMapping(value = "/emp/{empid}",
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Employee> findEmployee(@PathVariable Long empid)
    {
        logger.info(String.format("REST request to get Employee : {%s}", empid));
        Employee emp = employeeRepository.findOne(empid);

        return Optional.ofNullable(emp)
                .map(result -> new ResponseEntity<>(
                        result,
                        HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @RequestMapping(value = "/empwithjob/{job}",
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Employee> findEmployeeByJob(@PathVariable String job)
    {
        List<Employee> emp = employeeRepository.findByJob(job.toUpperCase());

        return emp;
    }

    @RequestMapping(value = "/emp/{empid}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long empid)
    {
        logger.info(String.format("REST request to delete Employee : {%s}", empid));
        employeeRepository.delete(empid);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("employee", empid.toString())).build();
    }

    @RequestMapping(value = "/emps",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Employee> updateEmployee(@Valid @RequestBody Employee employee) throws URISyntaxException
    {
        logger.info(String.format("REST request to update Employee : {%s}", employee));
        if (employee.getId() == null)
        {
            return createEmployee(employee);
        }

        Employee result = employeeRepository.save(employee);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert("employee", employee.getId().toString()))
                .body(result);
    }

    @RequestMapping(value = "/emps",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Employee> createEmployee(@Valid @RequestBody Employee employee) throws URISyntaxException
    {
        logger.info(String.format("REST request to save Employee : {%s}",  employee));
        if (employee.getId() != null) {
            return ResponseEntity.badRequest().headers
                    (HeaderUtil.createFailureAlert("employee", "idexists", "A new employee cannot already have an ID")).body(null);
        }
        Employee result = employeeRepository.save(employee);
        return ResponseEntity.created(new URI("/emps/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert("employee", result.getId().toString()))
                .body(result);
    }
}
