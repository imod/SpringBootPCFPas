package pivotal.au.apples.demo.repository;

import pivotal.au.apples.demo.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long>
{
    @Query("select e from Employee e where e.job = ?1")
    List<Employee> findByJob(String job);
}
