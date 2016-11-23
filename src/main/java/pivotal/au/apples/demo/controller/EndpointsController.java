package pivotal.au.apples.demo.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import pivotal.au.apples.demo.util.Utils;

@Controller
public class EndpointsController
{
    private static Log logger = LogFactory.getLog(EndpointsController.class);

    @RequestMapping(value = "/endpoints", method = RequestMethod.GET)
    public String homePage(Model model) throws Exception
    {
        logger.info("Invoking Endpoints Controller...");

        model.addAttribute("appIndex", Utils.applicationIndex());
        model.addAttribute("dbservice", Utils.getDBService());

        return "endpoints";
    }
}
