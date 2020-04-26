package kz.iitu.csse.group34.controllers;

import kz.iitu.csse.group34.entities.*;
import kz.iitu.csse.group34.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Controller
public class MainController {
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ItemsRepository itemsRepository;
    @Autowired
    private BooksRepository booksRepository;
    @Autowired
    private RolesRepository rolesRepository;
    @Autowired
    private GenresRepository genresRepository;
    @Autowired
    private OrdersRepository ordersRepository;
    @Autowired
    private UserRepository userRepository;

    @GetMapping(value = "/")
    public String index(ModelMap model, @RequestParam(name = "page", defaultValue = "1") int page){

        int pageSize = 10;

        if(page<1){
            page = 1;
        }

        int totalItems = booksRepository.countAllByDeletedAtNull();
        int tabSize = (totalItems+pageSize-1)/pageSize;

        Pageable pageable = PageRequest.of(page-1, pageSize);
        List<Books> books = booksRepository.findAllByDeletedAtNull(pageable);
        model.addAttribute("books", books);
        model.addAttribute("tabSize", tabSize);
        return "index";
    }

    @GetMapping(value = "/registration")
    public String registration(ModelMap model){

        return "registration";
    }
    @GetMapping(value = "addUser")
    public String addUser(ModelMap model){
        List<Roles> roles = rolesRepository.findAll();
        model.addAttribute("roles", roles);
        return "/admin/users/userAdd";
    }
    @PostMapping(value = "/addUser")
    public String addUser(
            @RequestParam(name = "email") String email,
            @RequestParam(name = "password") String password,
            @RequestParam(name = "rePassword") String rePassword,
            @RequestParam(name = "fullName") String fullName,
            @RequestParam(name = "roles", required = false) Long [] role,
            RedirectAttributes redirectAttrs
    ){
        Users userCheck = userRepository.findByEmail(email);
        if(userCheck != null){
            redirectAttrs.addAttribute("message", "This email is already used");
            return "redirect:/registration";
        }
        if(!(password.equals(rePassword))){
            redirectAttrs.addAttribute("message", "Passwords are not equal");
            return "redirect:/registration";
        }
        HashSet<Roles> roles = new HashSet<>();

        if(role !=null) {
            for (int i = 0; i < role.length; i++)
                roles.add(rolesRepository.findById(role[i]).get());
            Users user = new Users(email, passwordEncoder.encode(password), fullName, roles);
            userRepository.save(user);
            return "redirect:users";
        }
        else {
            roles.add(rolesRepository.findById(2L).orElse(null));
            Users user = new Users(email, passwordEncoder.encode(password), fullName, roles);
            userRepository.save(user);
            return "redirect:/login";
        }
    }
    @GetMapping(path = "addBook")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    public String addBook(ModelMap model){

        List<Genres> genres = genresRepository.findAll();
        model.addAttribute("genres", genres);

        return "admin/books/addBook";
    }
    @PostMapping(value = "/addBook")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    public String addBook(
            @RequestParam(name = "name") String name,
            @RequestParam(name = "price") int price,
            @RequestParam(name = "author") String author,
            @RequestParam(name = "description") String description,
            @RequestParam(name = "genre") Long [] genre
    ){
        HashSet<Genres> genres = new HashSet<>();
//        String[] arr = genre.split(",");
//        System.out.println(genre);
//        System.out.println(arr);
        for(int i =0;i<genre.length;i++){
            genres.add(genresRepository.findById(genre[i]).get());
        }
        Books book = new Books(name, price, author, description, new Date(), genres);
        booksRepository.save(book);

        return "redirect:/books";
    }

    @GetMapping(path = "editBook/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    public String editBook(ModelMap model,
                           @PathVariable(name = "id") Long id
    ){

        List<Genres> genres = genresRepository.findAll();
        Books book = booksRepository.findById(id).get();
        model.addAttribute("genres", genres);
        model.addAttribute("book", book);
        return "admin/books/editBook";
    }

    @PostMapping(value = "/editBook")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    public String editBook(
            @RequestParam(name = "id") Long id,
            @RequestParam(name = "name") String name,
            @RequestParam(name = "price") int price,
            @RequestParam(name = "author") String author,
            @RequestParam(name = "description") String description,
            @RequestParam(name = "genre") Long [] genre
    ){
        HashSet<Genres> genres = new HashSet<>();
//        String[] arr = genre.split(",");
//        System.out.println(genre);
//        System.out.println(arr);
        for(int i =0;i<genre.length;i++){
            genres.add(genresRepository.findById(genre[i]).get());
        }
        Books book = booksRepository.findById(id).get();
        book.setName(name);
        book.setPrice(price);
        book.setAuthor(author);
        book.setDescription(description);
        book.setGenres(genres);
        book.setUpdatedAt(new Date());
        booksRepository.save(book);

        return "redirect:/books";
    }
    @GetMapping(path = "editUser/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    public String editUser(ModelMap model,
                           @PathVariable(name = "id") Long id
    ){

        List<Roles> roles = rolesRepository.findAll();
        Users user = userRepository.findById(id).get();
        model.addAttribute("roles", roles);
        model.addAttribute("user", user);
        return "admin/users/userEdit";
    }
    @GetMapping(path = "/editPassword")
    public String editPassword(ModelMap model
    ){
        model.addAttribute("user", getUserData());
        return "editPassword";
    }
    @PostMapping(value = "/editPassword")
    public String editPassword(
            @RequestParam(name = "id") Long id,
            @RequestParam(name = "oldPassword") String oldPassword,
            @RequestParam(name = "password") String password,
            @RequestParam(name = "rePassword") String rePassword,
            @RequestParam(name = "fullName") String fullName,
            RedirectAttributes redirectAttrs
    ){
        Users user = userRepository.findById(id).get();
        if(password.equals(rePassword) && oldPassword.equals(user.getPassword())){
            user.setFullName(fullName);
            user.setPassword(passwordEncoder.encode(password));
            user.setUpdatedAt(new Date());
            userRepository.save(user);

            return "redirect:profile";
        }
        else{
            redirectAttrs.addAttribute("message", "Passwords are not equal, or wrong old password");
            return "redirect:/editPassword";
        }

    }
    @PostMapping(value = "/editUser")
    public String editUser(
            @RequestParam(name = "id") Long id,
            @RequestParam(name = "password") String password,
            @RequestParam(name = "rePassword") String rePassword,
            @RequestParam(name = "fullName") String fullName,
            @RequestParam(name = "roles") Long [] role,
            RedirectAttributes redirectAttrs
    ){
        Users user = userRepository.findById(id).get();
        HashSet<Roles> roles = new HashSet<>();
//        String[] arr = genre.split(",");
//        System.out.println(genre);
//        System.out.println(arr);
        for(int i =0;i<role.length;i++){
            roles.add(rolesRepository.findById(role[i]).get());
        }
        if(password.equals(rePassword)){
            user.setFullName(fullName);
            user.setPassword(passwordEncoder.encode(password));
            user.setRoles(roles);
            user.setUpdatedAt(new Date());
            userRepository.save(user);

            return "redirect:users";
        }
        else{
            redirectAttrs.addAttribute("message", "Passwords are not equal");
            return "redirect:editUser/" + id;
        }

    }
    @PostMapping(path = "/addOrder")
    @PreAuthorize("isAuthenticated()")
    public String addOrder(@RequestParam(name = "id") Long id,
                           RedirectAttributes redirectAttrs){
        Orders order = ordersRepository.findByUser_IdAndBook_IdAndDeletedAtNull(getUserData().getId(), id).orElse(new Orders(booksRepository.findByIdAndDeletedAtNull(id).get(), getUserData(), 1, 0));

        order.setCount(order.getCount() +1);
        ordersRepository.save(order);

        redirectAttrs.addAttribute("message", "Bought!");
        return "redirect:/details/" + id;
    }


    @PostMapping(value = "/add")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    public String add(
            @RequestParam(name = "name") String name,
            @RequestParam(name = "price") int price
    ){

        Items item = new Items(name, price);
        itemsRepository.save(item);

        return "redirect:/";
    }

    @GetMapping(path = "/details/{id}")
    public String details(ModelMap model, @PathVariable(name = "id") Long id){

        Optional<Books> book = booksRepository.findByIdAndDeletedAtNull(id);
        model.addAttribute("book", book.orElse(new Books("No Name", 0, "null", "null", new Date() ,null)));

        return "details";
    }

    @PostMapping(path = "/delete")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    public String delete(@RequestParam(name = "id") Long id){
        Items item = itemsRepository.findByIdAndDeletedAtNull(id).get();
        item.setDeletedAt(new Date());
        itemsRepository.save(item);
        return "redirect:/";
    }

    @GetMapping(path = "/login")
    public String loginPage(Model model){

        return "login";

    }

    @GetMapping(path = "/profile")
    @PreAuthorize("isAuthenticated()")
    public String profilePage(Model model){
        Users user = getUserData();
        List<Orders> orders2 = ordersRepository.findAllByUser_IdAndDeletedAtNull(user.getId());
        List<Orders> orders = ordersRepository.findAllByUser_IdAndDeletedAtNull(user.getId());
        for (Orders order : orders2) {
            if(order.getBook().getDeletedAt()!=null)
                orders.remove(order);
        }
        int total = 0;
        for (Orders order : orders) {
            total = total + order.getBook().getPrice() * order.getCount();
        }
        model.addAttribute("orders", orders);
        model.addAttribute("total", total);
        model.addAttribute("user", getUserData());
        return "profile";

    }

    @GetMapping(path = "users")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    public String usersPage(Model model){

        model.addAttribute("user", getUserData());
        List<Users> user2 = userRepository.findAll();
        List<Users> users = userRepository.findAll();
        for (Users user : user2) {
            if(user.getDeletedAt()!=null)
                users.remove(user);
        }
        model.addAttribute("userList", users);

        return "admin/users/users";

    }
    @GetMapping(path = "orders")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    public String orders(Model model, @RequestParam(name = "page", defaultValue = "1") int page){
        int pageSize = 10;

        if(page<1){
            page = 1;
        }

        int totalItems = ordersRepository.countAllByDeletedAtNull();
        int tabSize = (totalItems+pageSize-1)/pageSize;

        Pageable pageable = PageRequest.of(page-1, pageSize);
        List<Orders> orders = ordersRepository.findAllByDeletedAtNull(pageable);
        model.addAttribute("orders", orders);
        model.addAttribute("tabSize", tabSize);

        return "admin/orders/orders";

    }
    @GetMapping(path = "books")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    public String booksPage(Model model, @RequestParam(name = "page", defaultValue = "1") int page){
        int pageSize = 10;

        if(page<1){
            page = 1;
        }

        int totalItems = booksRepository.countAllByDeletedAtNull();
        int tabSize = (totalItems+pageSize-1)/pageSize;

        Pageable pageable = PageRequest.of(page-1, pageSize);
        List<Books> books = booksRepository.findAllByDeletedAtNull(pageable);
        model.addAttribute("books", books);
        model.addAttribute("tabSize", tabSize);

        return "admin/books/books";

    }
    @PostMapping(path ="/deleteUser")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    public String deleteUser(@RequestParam(name = "id") Long id){
        Users user = userRepository.findByIdAndDeletedAtNull(id).get();
        List<Orders> orders = ordersRepository.findAllByUser_IdAndDeletedAtNull(id);
        for (Orders order:orders) {
            order.setDeletedAt(new Date());
        }
        user.setDeletedAt(new Date());
        user.setPassword("i am sorry for this"); // i am sorry for this

        userRepository.save(user);
        return "redirect:/users";
    }
    @PostMapping(path = "/deleteBook")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    public String deleteBook(@RequestParam(name = "id") Long id){
        Books book = booksRepository.findByIdAndDeletedAtNull(id).get();
        List<Orders> orders = ordersRepository.findAllByBook_IdAndDeletedAtNull(id);
        for (Orders order:orders) {
            order.setDeletedAt(new Date());
        }
        book.setDeletedAt(new Date());
        booksRepository.save(book);
        return "redirect:/books";
    }
    @PostMapping(path = "/deleteOrder")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    public String deleteOrder(@RequestParam(name = "id") Long id){
        Orders order = ordersRepository.findByIdAndDeletedAtNull(id).get();

        order.setDeletedAt(new Date());
        ordersRepository.save(order);
        return "redirect:/orders";
    }


    public Users getUserData(){
        Users userData = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(!(authentication instanceof AnonymousAuthenticationToken)){
            User secUser = (User)authentication.getPrincipal();
            userData = userRepository.findByEmail(secUser.getUsername());
        }
        return userData;
    }

}