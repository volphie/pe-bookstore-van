package cnabookstore;

import org.springframework.web.bind.annotation.*;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;

@RestController
public class VanController {

    boolean flag;

    public VanController(){
        flag = true;
    }

    @GetMapping("/requestPayment")
    @HystrixCommand(fallbackMethod = "fallBackPayment", commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "5000"),
            @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds", value = "10000")
    })
    public @ResponseBody String requestPayment(@RequestParam long payment) throws InterruptedException {
        System.out.println("@@@ requestPayment!!!");
        if (payment == 0) {
            System.out.println("@@@ CircuitBreaker!!!");
            Thread.sleep(10000);
            return "PAYMENT_FAILED";
        } else {
            System.out.println("@@@ Success!!!");
            return "PAYMENT_COMPLETED";
        }

    }

    public String fallBackPayment(long payment ){
        System.out.println("### fallback!!!");
        return "PAYMENT_FAILED";
    }

    @GetMapping("/isHealthy")
    public void test() throws Exception {
        if (flag) {
            System.out.println("health.... !!!");
        }
        else{
            throw new Exception("Zombie...");
        }
    }

    @GetMapping("/makeZombie")
    public void zombie(){
        flag = false;
    }

    @GetMapping("/isReady")
    public void ready(){
        System.out.println("System is ready.... !!!");
    }

}
