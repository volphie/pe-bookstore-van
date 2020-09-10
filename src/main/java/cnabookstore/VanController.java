package cnabookstore;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;

@RestController
public class VanController {


    @GetMapping("/requestPayment")
    @HystrixCommand(fallbackMethod = "fallBackPayment", commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "5000"),
            @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds", value = "10000")
    })
    public void requestPayment(@RequestParam long payment) throws InterruptedException {
        System.out.println("@@@ requestPayment!!!");
        if (payment == 0) {
            System.out.println("@@@ CircuitBreaker!!!");
            Thread.sleep(10000);
            //throw new RuntimeException("CircuitBreaker!!!");
        } else {
            System.out.println("@@@ Success!!!");
        }
    }

    public void fallBackPayment(long payment ){
        System.out.println("### fallback!!!");
//        return "CircuitBreaker!!!";
    }
    @GetMapping("/test")
    public void test(){
        System.out.println("test !!!");
    }

}
