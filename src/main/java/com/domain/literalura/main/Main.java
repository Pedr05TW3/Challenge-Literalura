package com.domain.literalura.main;

import com.domain.literalura.model.*;
import com.domain.literalura.repository.*;
import com.domain.literalura.service.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class Main {
    private final Scanner keyboard = new Scanner(System.in);
    private final ApiConsulter apiConsulter = new ApiConsulter();
    private final DataConverter dataConverter = new DataConverter();
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    public Main(BookRepository bookRepository, AuthorRepository authorRepository) { this.bookRepository = bookRepository; this.authorRepository = authorRepository; }

    public void start() {
        var option = -1;

        while (option != 0) {
            var menu = """
                  
                    --- Escolha um opção: ---
                    
                    1 - Buscar Livros pelo Título
                    2 - Listar Livros Buscados
                    3 - Listar Autores Buscados
                    4 - Listar Autores vivos em um ano específico
                    5 - Listar Livros por idioma
                    6 - Top 10 Livros mais baixados
                    7 - Estatísticas de Downloads dos Livros Pesquisados
                                        
                    0 - Fechar
                    """;

            System.out.println(menu);

            if (keyboard.hasNextInt()) {
                option = keyboard.nextInt();
                keyboard.nextLine();

                switch (option) {
                    case 1:
                        searchBookByTitle();
                        break;
                    case 2:
                        listRegisteredBooks();
                        break;
                    case 3:
                        listRegisteredAuthors();
                        break;
                    case 4:
                        ListAuthorsAliveInAGivenYear();
                        break;
                    case 5:
                        listBooksByLanguage();
                        break;
                    case 6:
                        listTop10();
                        break;
                    case 7:
                        showDbStatistics();
                        break;
                    case 0:
                        System.out.println("\nEncerrando...");
                        break;
                    default:
                        System.out.println("\nOpção Inválida");
                }

            } else {
                System.out.println("\nEscolha uma das opções!");
                keyboard.next();
            }
        }
    }

    @Transactional
    private void searchBookByTitle() {
        String BASE_URL = "https://gutendex.com/books/?search=";
        System.out.println("\nDigite o nome do Livro: ");
        var title = keyboard.nextLine();

        if (!title.isBlank() && !isANumber(title)) {

            var json = apiConsulter.obtainData(BASE_URL + title.replace(" ", "%20"));
            var data = dataConverter.obtainData(json, Data.class);
            Optional<BookData> searchBook = data.results()
                    .stream()
                    .filter(b -> b.title().toLowerCase().contains(title.toLowerCase()))
                    .findFirst();

            if (searchBook.isPresent()) {
                BookData bookData = searchBook.get();

                if (!verifiedBookExistence(bookData)) {
                    Book book = new Book(bookData);
                    AuthorData authorData = bookData.author().get(0);
                    Optional<Author> optionalAuthor = authorRepository.findByName(authorData.name());

                    if (optionalAuthor.isPresent()) {
                        Author existingAuthor = optionalAuthor.get();
                        book.setAuthor(existingAuthor);
                        existingAuthor.getBooks().add(book);
                        authorRepository.save(existingAuthor);
                    } else {
                        Author newAuthor = new Author(authorData);
                        book.setAuthor(newAuthor);
                        newAuthor.getBooks().add(book);
                        authorRepository.save(newAuthor);
                    }

                    bookRepository.save(book);

                } else {
                    System.out.println("\nLivro já está cadastrado");
                }

            } else {
                System.out.println("\nLivro não encontrado, tente novamente!");
            }

        } else {
            System.out.println("\nO nome foi digitado incorretamente!");
        }

    }

    private void listRegisteredBooks() {
        List<Book> books = bookRepository.findAll();

        if(!books.isEmpty()) {
            System.out.println("\n----- Livros -----");
            books.forEach(System.out::println);
        } else {
            System.out.println("\nNão há nenhum livro cadastrado");
        }

    }

    private void listRegisteredAuthors() {
        List<Author> authors = authorRepository.findAll();

        if(!authors.isEmpty()) {
            System.out.println("\n----- Autores -----");
            authors.forEach(System.out::println);
        } else {
            System.out.println("\nNão há nenhum autor cadastrado");
        }

    }

    private boolean verifiedBookExistence(BookData bookData) {
        Book book = new Book(bookData);
        return bookRepository.verifiedBDExistence(book.getTitle());
    }

    private void ListAuthorsAliveInAGivenYear() {
        System.out.println("\nDigite o ano a ser consultado: ");

        if (keyboard.hasNextInt()) {
            var year = keyboard.nextInt();
            List<Author> authors = authorRepository.findAuthorsAlive(year);

            if (!authors.isEmpty()) {
                System.out.println("\n----- Autores vivos em: " + year + " -----");
                authors.forEach(System.out::println);
            } else {
                System.out.println("\nNenhum autor encontrado para esse período");
            }

        } else {
            System.out.println("\nAno digitado incorretamente, tente novamente!");
            keyboard.next();
        }

    }

    private void listBooksByLanguage() {
        var option = -1;
        String language = "";

        System.out.println("\nEscolha o idioma a ser consultado: ");
        var languagesMenu = """
               \n
               1 - Inglês
               2 - Francês
               3 - Alemão
               4 - Português
               5 - Espanhol
               """;

        System.out.println(languagesMenu);

        if (keyboard.hasNextInt()) {
            option = keyboard.nextInt();

            switch (option) {
                case 1:
                    language = "en";
                    break;
                case 2:
                    language = "fr";
                    break;
                case 3:
                    language = "de";
                    break;
                case 4:
                    language = "pt";
                    break;
                case 5:
                    language = "es";
                    break;
                default:
                    System.out.println("\nEscolha uma das opções!");
            }

            System.out.println("\nLivros Cadastrados:");
            List<Book> livros = bookRepository.findBooksByLanguage(language);

            if (!livros.isEmpty()) {
                livros.forEach(System.out::println);
            } else {
                System.out.println("\nNenhum livro encontrado, selecione outro idioma!");
            }

        } else {
            System.out.println("\nOpção inválida!");
            keyboard.next();
        }

    }

    private boolean isANumber(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void listTop10() {
        List<Book> books = bookRepository.findTop10();

        if (!books.isEmpty()) {
            System.out.println("\n----- Top 10 downloaded books -----");
            books.forEach(b -> System.out.println(b.getTitle()));
        } else {
            System.out.println("\nNenhum livro encontrado!");
        }

    }

    private void showDbStatistics() {
        List<Book> books = bookRepository.findAll();

        if (!books.isEmpty()) {
            IntSummaryStatistics sta = books.stream()
                    .filter(b -> b.getDownloads() > 0)
                    .collect(Collectors.summarizingInt(Book::getDownloads));

            System.out.println("\n----- Estatísticas de Downloads -----");
            System.out.println("Average downloads: " + sta.getAverage());
            System.out.println("Max downloads: " + sta.getMax());
            System.out.println("Min downloads: " + sta.getMin());
            System.out.println("Registered book/s: " + sta.getCount());
        } else {
            System.out.println("\nNenhum livro encontrado!");
        }

    }

}
