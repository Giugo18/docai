import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PaginaChat } from './pagina-chat';

describe('PaginaChat', () => {
  let component: PaginaChat;
  let fixture: ComponentFixture<PaginaChat>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaginaChat],
    }).compileComponents();

    fixture = TestBed.createComponent(PaginaChat);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
