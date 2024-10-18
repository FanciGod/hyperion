import { Component, OnInit } from '@angular/core';
import { SafeHtml, DomSanitizer } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { Blogs } from '../../../dto/Blog';
import { BlogService } from '../../service/blog.service';


@Component({
  selector: 'app-blog',
  templateUrl: './blog.component.html',
  styleUrl: './blog.component.scss'
})
export class BlogComponent implements OnInit {
  blogId!: number;
  blogCategoryName!: string;
  blogDetail: Blogs | undefined;
  description!: SafeHtml;
  constructor(private route: ActivatedRoute, private blogService: BlogService, private sanitizer: DomSanitizer, private router: Router) { }

  ngOnInit(): void {
    this.route.params.subscribe(param => {
      this.blogId = +param['blogId'];
      this.blogCategoryName = param['blogCategoryName'];
      // this.getNewsById(this.newsId);
      this.getBlogById(this.blogId);
    })
  }

  getBlogById(id: number) {
    this.blogService.getBlogById(id).subscribe(res => {
      this.blogDetail = res.result;
      if (this.blogDetail.blogCategoryDto.name != this.blogCategoryName) {
        this.router.navigate(['not-found']);
      }
      const rawDescription = this.blogDetail.description;
      this.description = this.sanitizer.bypassSecurityTrustHtml(rawDescription);
    })
  }
}
